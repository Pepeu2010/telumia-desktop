package com.nuvio.app.features.membership

import co.touchlab.kermit.Logger
import com.nuvio.app.core.network.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val ProfileBackgroundBucket = "membership-profile-backgrounds"

data class ProfileBackgroundCatalogItem(
    val id: String,
    val displayName: String,
    val assetVersion: Int,
    val landscapeImageUri: String? = null,
    val landscapeImageBytes: ByteArray? = null,
    val portraitImageUri: String? = null,
    val portraitImageBytes: ByteArray? = null,
)

object ProfileBackgroundRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val backgroundDownloadSemaphore = Semaphore(2)
    private val memoryLock = Any()
    private val fallbackMemoryBudget = ProfileBackgroundMemoryBudget(
        ProfileBackgroundMemoryPolicy.maxRetainedBytes(MemberAssetStorage.profileBackgroundDeviceMemoryBytes()),
    )
    private val log = Logger.withTag("ProfileBackgroundRepository")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _catalog = MutableStateFlow<List<ProfileBackgroundCatalogItem>>(emptyList())
    val catalog: StateFlow<List<ProfileBackgroundCatalogItem>> = _catalog.asStateFlow()
    private var loadJob: Job? = null
    private var assetLoadJob: Job? = null
    private var remoteCatalog = emptyList<SupabaseProfileBackgroundCatalogItem>()
    private var cacheHydrated = false
    private var remoteLoaded = false

    fun ensureLoaded() {
        hydrateFromCacheIfNeeded()
        if (remoteLoaded || loadJob?.isActive == true) return
        loadJob = scope.launch {
            try {
                val items = SupabaseProvider.client.postgrest
                    .rpc("get_member_profile_background_catalog")
                    .decodeList<SupabaseProfileBackgroundCatalogItem>()
                remoteCatalog = items
                publishMetadata(items)
                saveCachedCatalog(items)
                remoteLoaded = true
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                log.w(error) { "Unable to load supporter profile backgrounds" }
            }
        }
    }

    fun loadSelectedAndPreload(id: String, portrait: Boolean) {
        ensureLoaded()
        assetLoadJob?.cancel()
        assetLoadJob = scope.launch {
            val initialSelected = remoteCatalog.firstOrNull { it.id == id }
            initialSelected?.let { loadAndPublish(it, portrait) }
            loadJob?.join()
            val selected = remoteCatalog.firstOrNull { it.id == id } ?: return@launch
            if (selected.assetVersion != initialSelected?.assetVersion) {
                loadAndPublish(selected, portrait)
            }
        }
    }

    fun preloadLandscapeImages() {
        ensureLoaded()
        assetLoadJob?.cancel()
        assetLoadJob = scope.launch {
            loadJob?.join()
            remoteCatalog.forEach { item -> loadCachedAndPublish(item, portrait = false) }
        }
    }

    fun invalidate() {
        cacheHydrated = false
        remoteLoaded = false
        loadJob?.cancel()
        loadJob = null
        assetLoadJob?.cancel()
        assetLoadJob = null
        remoteCatalog = emptyList()
        synchronized(memoryLock) {
            fallbackMemoryBudget.clear()
            _catalog.value = emptyList()
        }
    }

    private fun hydrateFromCacheIfNeeded() {
        if (cacheHydrated) return
        cacheHydrated = true
        val payload = MemberAssetStorage.loadProfileBackgroundCatalogPayload().orEmpty().trim()
        if (payload.isEmpty()) return
        val stored = runCatching {
            json.decodeFromString<StoredProfileBackgroundCatalogPayload>(payload)
        }.getOrNull() ?: return
        remoteCatalog = stored.items
        publishMetadata(stored.items)
    }

    private fun publishMetadata(items: List<SupabaseProfileBackgroundCatalogItem>) = synchronized(memoryLock) {
        val current = _catalog.value.associateBy(ProfileBackgroundCatalogItem::id)
        var next = items.map { item ->
            val cached = current[item.id]?.takeIf { it.assetVersion == item.assetVersion }
            ProfileBackgroundCatalogItem(
                id = item.id,
                displayName = item.displayName,
                landscapeImageUri = cached?.landscapeImageUri,
                landscapeImageBytes = cached?.landscapeImageBytes,
                portraitImageUri = cached?.portraitImageUri,
                portraitImageBytes = cached?.portraitImageBytes,
                assetVersion = item.assetVersion,
            )
        }
        fallbackMemoryBudget.clear()
        for (item in next) {
            item.landscapeImageBytes?.let { bytes ->
                val evicted = fallbackMemoryBudget.record(memoryKey(item, portrait = false), bytes.size.toLong())
                next = clearEvictedImages(next, evicted)
            }
            item.portraitImageBytes?.let { bytes ->
                val evicted = fallbackMemoryBudget.record(memoryKey(item, portrait = true), bytes.size.toLong())
                next = clearEvictedImages(next, evicted)
            }
        }
        _catalog.value = next
    }

    private fun saveCachedCatalog(items: List<SupabaseProfileBackgroundCatalogItem>) {
        MemberAssetStorage.saveProfileBackgroundCatalogPayload(
            json.encodeToString(StoredProfileBackgroundCatalogPayload(items)),
        )
    }

    private suspend fun loadCachedAndPublish(item: SupabaseProfileBackgroundCatalogItem, portrait: Boolean) {
        val image = withContext(Dispatchers.IO) {
            val landscape = MemberAssetStorage.loadProfileBackgroundUri("${item.id}-v${item.assetVersion}")
                ?.let { ProfileBackgroundImageSource(uri = it) }
            if (portrait) {
                item.portraitStoragePath
                    ?.let { MemberAssetStorage.loadProfileBackgroundUri("${item.id}-portrait-v${item.assetVersion}") }
                    ?.let { ProfileBackgroundImageSource(uri = it) }
                    ?: landscape
            } else {
                landscape
            }
        } ?: return
        publishImage(item, portrait, image)
    }

    private suspend fun loadAndPublish(item: SupabaseProfileBackgroundCatalogItem, portrait: Boolean) {
        try {
            val alreadyLoaded = loadedImage(item, portrait)
            if (alreadyLoaded != null) {
                if (alreadyLoaded.bytes != null) synchronized(memoryLock) {
                    fallbackMemoryBudget.touch(memoryKey(item, portrait))
                }
                return
            }
            val image = if (portrait) loadPortraitImage(item) else loadLandscapeImage(item)
            image?.let { publishImage(item, portrait, it) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            log.w(error) { "Unable to load supporter profile background ${item.id}" }
        }
    }

    private fun publishImage(
        item: SupabaseProfileBackgroundCatalogItem,
        portrait: Boolean,
        image: ProfileBackgroundImageSource,
    ) = synchronized(memoryLock) {
        val catalog = _catalog.value
        if (catalog.none { it.id == item.id && it.assetVersion == item.assetVersion }) return@synchronized
        val key = memoryKey(item, portrait)
        val evicted = image.bytes?.let { fallbackMemoryBudget.record(key, it.size.toLong()) }
            ?: run { fallbackMemoryBudget.remove(key); emptyList() }
        var next = catalog.map { background ->
            if (background.id != item.id || background.assetVersion != item.assetVersion) {
                background
            } else if (portrait) {
                background.copy(portraitImageUri = image.uri, portraitImageBytes = image.bytes)
            } else {
                background.copy(landscapeImageUri = image.uri, landscapeImageBytes = image.bytes)
            }
        }
        next = clearEvictedImages(next, evicted)
        _catalog.value = next
    }

    private suspend fun loadPortraitImage(item: SupabaseProfileBackgroundCatalogItem): ProfileBackgroundImageSource? {
        val portraitPath = item.portraitStoragePath ?: return loadLandscapeImage(item)
        return try {
            loadImage(
                cacheKey = "${item.id}-portrait-v${item.assetVersion}",
                storagePath = portraitPath,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            log.w(error) { "Unable to load portrait background ${item.id}" }
            loadLandscapeImage(item)
        }
    }

    private suspend fun loadLandscapeImage(item: SupabaseProfileBackgroundCatalogItem): ProfileBackgroundImageSource? =
        loadImage(
            cacheKey = "${item.id}-v${item.assetVersion}",
            storagePath = item.storagePath,
        )

    private suspend fun loadImage(cacheKey: String, storagePath: String): ProfileBackgroundImageSource? {
        withContext(Dispatchers.IO) {
            MemberAssetStorage.loadProfileBackgroundUri(cacheKey)
        }?.let { return ProfileBackgroundImageSource(uri = it) }
        val bytes = backgroundDownloadSemaphore.withPermit {
            SupabaseProvider.client.storage[ProfileBackgroundBucket].downloadAuthenticated(storagePath)
        }
        if (bytes.isEmpty() || bytes.size.toLong() > MAX_PROFILE_BACKGROUND_BYTES) return null
        val uri = try {
            withContext(Dispatchers.IO) { MemberAssetStorage.saveProfileBackground(cacheKey, bytes) }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
        return if (uri != null) ProfileBackgroundImageSource(uri = uri) else ProfileBackgroundImageSource(bytes = bytes)
    }

    private fun loadedImage(
        item: SupabaseProfileBackgroundCatalogItem,
        portrait: Boolean,
    ): ProfileBackgroundImageSource? = _catalog.value.firstOrNull {
        it.id == item.id && it.assetVersion == item.assetVersion
    }?.let { background ->
        if (portrait) {
            (background.portraitImageUri?.let { ProfileBackgroundImageSource(uri = it) }
                ?: background.portraitImageBytes?.let { ProfileBackgroundImageSource(bytes = it) })
        } else {
            (background.landscapeImageUri?.let { ProfileBackgroundImageSource(uri = it) }
                ?: background.landscapeImageBytes?.let { ProfileBackgroundImageSource(bytes = it) })
        }
    }

    private fun memoryKey(item: ProfileBackgroundCatalogItem, portrait: Boolean) =
        ProfileBackgroundMemoryKey(item.id, item.assetVersion, portrait)

    private fun memoryKey(item: SupabaseProfileBackgroundCatalogItem, portrait: Boolean) =
        ProfileBackgroundMemoryKey(item.id, item.assetVersion, portrait)

    private fun clearEvictedImages(
        catalog: List<ProfileBackgroundCatalogItem>,
        evicted: List<ProfileBackgroundMemoryKey>,
    ): List<ProfileBackgroundCatalogItem> {
        if (evicted.isEmpty()) return catalog
        val evictedKeys = evicted.toSet()
        return catalog.map { item ->
            item.copy(
                landscapeImageBytes = item.landscapeImageBytes.takeUnless {
                    memoryKey(item, portrait = false) in evictedKeys
                },
                portraitImageBytes = item.portraitImageBytes.takeUnless {
                    memoryKey(item, portrait = true) in evictedKeys
                },
            )
        }
    }

    private data class ProfileBackgroundImageSource(val uri: String? = null, val bytes: ByteArray? = null) {
        init { require((uri == null) != (bytes == null)) }
    }

    private const val MAX_PROFILE_BACKGROUND_BYTES = 8L * 1024L * 1024L
}

@Serializable
private data class StoredProfileBackgroundCatalogPayload(
    val items: List<SupabaseProfileBackgroundCatalogItem> = emptyList(),
)

@Serializable
private data class SupabaseProfileBackgroundCatalogItem(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("portrait_storage_path") val portraitStoragePath: String? = null,
    @SerialName("asset_version") val assetVersion: Int,
)
