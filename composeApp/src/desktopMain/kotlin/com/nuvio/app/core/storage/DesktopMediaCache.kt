package com.nuvio.app.core.storage

import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.Executors
import com.sun.management.OperatingSystemMXBean

/** Device-local policy; no auth/profile sync payload is extended. Native runtime files are outside its root. */
internal object DesktopMediaCache {
    private val preferences = DesktopMediaCachePreferences(DesktopStorage.store("telumia_media_cache_v1"))
    val root: Path by lazy { DesktopStorage.cacheDir.resolve("media-cache-v1").also { Files.createDirectories(it) } }
    val activeSettings: MediaCacheSettings by lazy { loadSettings() }
    private val occupancyExecutor by lazy {
        Executors.newSingleThreadExecutor { task ->
            Thread(task, "telumia-media-cache-scan").apply { isDaemon = true; priority = Thread.NORM_PRIORITY - 1 }
        }
    }
    private val occupancyMonitor by lazy {
        MediaCacheOccupancyMonitor(
            scan = {
                // Versioned runtime assets count toward Auto storage estimates but are never media-cache eviction targets.
                val directFiles = DirectCacheFileOccupancy.measure(DesktopStorage.cacheDir, maxMillis = 25)
                val ownedCaches = OwnedMediaCacheOccupancy.measure(DesktopStorage.cacheDir,
                    MediaCacheCategory.entries.map { listOf("media-cache-v1", it.directoryName) } +
                        listOf(
                            listOf("gif-cache"),
                            listOf("member_profile_avatars"),
                            listOf("member_profile_backgrounds"),
                            listOf("quickjs"),
                            listOf("native-player-bridge"),
                            listOf("player-ui"),
                        ))
                // Continue Watching, stream links, and binge-group hints are regenerable caches in
                // dedicated preference files. Count only these known files, never durable app data.
                val continueWatchingSnapshots = OwnedMediaCacheOccupancy.measure(
                    DesktopStorage.rootDir,
                    listOf(
                        listOf("nuvio_continue_watching_enrichment.properties"),
                        listOf("nuvio_stream_link_cache.properties"),
                        listOf("nuvio_binge_group_cache.properties"),
                    ),
                    maxMillis = 25,
                )
                val parts = listOf(directFiles, ownedCaches, continueWatchingSnapshots)
                val totalBytes = parts.fold(0L) { total, part ->
                    if (total == Long.MAX_VALUE || part.bytes < 0 || total > Long.MAX_VALUE - part.bytes) {
                        Long.MAX_VALUE
                    } else total + part.bytes
                }
                MediaCacheOccupancy(
                    bytes = totalBytes,
                    visitedEntries = parts.sumOf { it.visitedEntries.toLong() }
                        .coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                    complete = parts.all { it.complete },
                )
            },
            executor = occupancyExecutor,
            onSnapshotUpdated = budgetMonitor::invalidate,
        )
    }
    private val budgetMonitor = MediaCacheBudgetMonitor(
        MediaCachePlatform.DESKTOP,
        { activeSettings },
        ::detectDevice,
        { System.nanoTime() / 1_000_000L },
    )
    val activeBudget: MediaCacheBudget get() = budgetMonitor.current().budget

    fun loadSettings(): MediaCacheSettings = preferences.load()
    fun saveSettings(settings: MediaCacheSettings) = preferences.save(settings)

    fun detectDevice(): MediaCacheDeviceSnapshot = MediaCacheDeviceSnapshot(
        memoryBytes = physicalMemoryBytes(),
        usableStorageBytes = runCatching { Files.getFileStore(root).usableSpace }.getOrNull(),
        occupiedCacheBytes = occupiedCache.bytes)

    fun physicalMemoryBytes(): Long? = runCatching {
        (ManagementFactory.getOperatingSystemMXBean() as? OperatingSystemMXBean)?.totalMemorySize
    }.getOrNull()

    internal val occupiedCache: MediaCacheOccupancy get() = occupancyMonitor.current()

    fun directory(category: MediaCacheCategory): Path = root.resolve(category.directoryName)
    val writesEnabled: Boolean get() = activeBudget.totalBytes > 0
    // Under the storage reserve, open existing Coil caches for reads with their requested capacity.
    // The loader's READ_ONLY policy suspends writes rather than deleting offline records.
    private fun coilBudget(): MediaCacheBudget = if (writesEnabled) activeBudget else activeBudget.copy(totalBytes = activeBudget.requestedBytes)
    private fun imageCacheAllocation() = MediaImageCachePolicy.allocate(
        coilBudget().quota(MediaCacheCategory.IMAGES), hasSeparateGifCache = true,
    )
    fun coilImageQuota(): Long = imageCacheAllocation().coilBytes
    fun coilPreviewQuota(): Long = coilBudget().quota(MediaCacheCategory.PREVIEWS)
    fun coilBadgeQuota(): Long = coilBudget().quota(MediaCacheCategory.BADGES)
    private fun imageMemoryCacheAllocation() = DesktopImageMemoryCachePolicy.allocate(physicalMemoryBytes())
    fun coilImageMemoryCacheBytes(): Long = imageMemoryCacheAllocation().imagesBytes
    fun coilBadgeMemoryCacheBytes(): Long = imageMemoryCacheAllocation().badgesBytes
    fun cacheQuota(category: MediaCacheCategory): Long = coilBudget().quota(category)
    fun memberBackgroundQuota(): Long = imageCacheAllocation().memberBackgroundBytes
    fun cacheWritesAllowed(category: MediaCacheCategory, openedQuotaBytes: Long): Boolean {
        val snapshot = budgetMonitor.current(forceRefresh = true)
        return MediaCachePolicy.allowsNetworkCacheWrite(
            snapshot.budget,
            category,
            openedQuotaBytes,
            snapshot.device.usableStorageBytes,
            MediaCachePlatform.DESKTOP.reserveBytes,
        )
    }

    fun canBeginCacheWrite(category: MediaCacheCategory): Boolean = MediaCachePolicy.canBeginCacheWrite(
        budget = activeBudget,
        category = category,
        freeStorageBytes = runCatching { Files.getFileStore(root).usableSpace }.getOrNull(),
        reserveBytes = MediaCachePlatform.DESKTOP.reserveBytes,
    )

    /** Exact-size preflight for bounded snapshots written outside the cache directory. */
    fun canWriteCacheEntry(category: MediaCacheCategory, bytes: Long, storagePath: Path): Boolean {
        if (bytes < 0) return false
        val snapshot = budgetMonitor.current(forceRefresh = true)
        val free = runCatching { Files.getFileStore(storagePath).usableSpace }.getOrNull()
        val reserve = MediaCachePlatform.DESKTOP.reserveBytes
        val availableAboveReserve = free?.takeIf { it > reserve }?.minus(reserve) ?: return false
        return snapshot.budget.totalBytes > 0 && snapshot.budget.quota(category) > 0 &&
            bytes <= availableAboveReserve
    }

    val gifCache: BoundedLegacyFileCache by lazy {
        BoundedLegacyFileCache(
            directory(MediaCacheCategory.IMAGES).resolve("gifs"),
            DesktopStorage.cacheDir.resolve("gif-cache"),
            quotaBytes = { imageCacheAllocation().gifBytes },
            maxFiles = 200,
            maxEntryBytes = 8L * MIB,
            canWrite = ::hasStorageFor,
            legacyFileName = { "${sha256(it)}.gif" },
            legacyName = Regex("[a-f0-9]{64}\\.gif"),
        )
    }

    val memberBackgroundCache: BoundedLegacyFileCache by lazy {
        BoundedLegacyFileCache(
            directory(MediaCacheCategory.IMAGES).resolve("member-backgrounds"),
            DesktopStorage.cacheDir.resolve("member_profile_backgrounds"),
            quotaBytes = ::memberBackgroundQuota,
            maxFiles = 64,
            maxEntryBytes = 8L * MIB,
            canWrite = ::hasStorageFor,
        )
    }

    val memberAvatarCache: BoundedLegacyFileCache by lazy {
        BoundedLegacyFileCache(
            directory(MediaCacheCategory.AVATARS).resolve("member-assets"),
            DesktopStorage.cacheDir.resolve("member_profile_avatars"),
            quotaBytes = { activeBudget.quota(MediaCacheCategory.AVATARS) },
            maxFiles = 256,
            maxEntryBytes = 2L * MIB,
            canWrite = ::hasStorageFor,
            legacyFileName = ::legacyAvatarFileName,
            legacyName = Regex("[A-Za-z0-9._-]{1,200}"),
        )
    }

    /** Migrate legacy GIFs on demand; no network request is required for an already cached image. */
    fun readGif(url: String): ByteArray? = gifCache.get(url)

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun legacyAvatarFileName(key: String): String? = key
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .takeIf { it.isNotEmpty() && it.length <= 200 && it != "." && it != ".." }

    private val fileCaches = mutableMapOf<MediaCacheCategory, BoundedFileCache>()
    fun files(category: MediaCacheCategory): BoundedFileCache = synchronized(fileCaches) {
        require(category != MediaCacheCategory.IMAGES && category != MediaCacheCategory.BADGES)
        fileCaches.getOrPut(category) { BoundedFileCache(directory(category).resolve("objects"),
            quotaBytes = { activeBudget.quota(category) }, canWrite = ::hasStorageFor) }
    }

    private fun hasStorageFor(bytes: Long): Boolean = runCatching {
        Files.getFileStore(root).usableSpace - bytes >= MediaCachePlatform.DESKTOP.reserveBytes
    }.getOrDefault(false)
}
