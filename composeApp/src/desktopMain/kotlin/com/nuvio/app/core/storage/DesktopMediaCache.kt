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
                OwnedMediaCacheOccupancy.measure(DesktopStorage.cacheDir,
                    MediaCacheCategory.entries.map { listOf("media-cache-v1", it.directoryName) } +
                        listOf(
                            listOf("gif-cache"),
                            listOf("member_profile_avatars"),
                            listOf("member_profile_backgrounds"),
                            listOf("quickjs"),
                            listOf("native-player-bridge"),
                            listOf("player-ui"),
                        ))
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
    fun cacheQuota(category: MediaCacheCategory): Long = coilBudget().quota(category)
    fun memberBackgroundQuota(): Long = imageCacheAllocation().memberBackgroundBytes
    fun cacheWritesAllowed(category: MediaCacheCategory, openedQuotaBytes: Long): Boolean =
        MediaCachePolicy.allowsNewWrites(activeBudget, category, openedQuotaBytes)

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
