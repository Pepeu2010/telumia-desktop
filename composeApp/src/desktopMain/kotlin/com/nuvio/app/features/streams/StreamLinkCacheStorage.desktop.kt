package com.nuvio.app.features.streams

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.DesktopMediaCache
import com.nuvio.app.core.storage.BoundedDesktopPreferenceCache
import com.nuvio.app.core.storage.DesktopMetadataPreferenceCachePolicy
import com.nuvio.app.core.storage.MediaCacheCategory
import com.nuvio.app.core.storage.ProfileScopedKey
import com.nuvio.app.core.storage.StreamLinkCacheTimestamp

internal actual object StreamLinkCacheStorage {
    private val store = DesktopStorage.store("nuvio_stream_link_cache")
    private val cache by lazy {
        BoundedDesktopPreferenceCache(
            store = store,
            namespacePrefix = "stream_link_",
            entryPrefix = "stream_link_",
            maxEntries = 512,
            quotaBytes = {
                if (!DesktopMediaCache.writesEnabled) 0L
                else DesktopMetadataPreferenceCachePolicy.allocate(
                    DesktopMediaCache.cacheQuota(MediaCacheCategory.METADATA),
                ).streamLinksBytes
            },
            canWriteEntry = { bytes ->
                DesktopMediaCache.canWriteCacheEntry(
                    MediaCacheCategory.METADATA,
                    bytes,
                    DesktopStorage.rootDir,
                )
            },
            savedAtMs = { _, payload -> StreamLinkCacheTimestamp.read(payload) },
        )
    }

    actual fun loadEntry(hashedKey: String): String? =
        cache.load(ProfileScopedKey.of(hashedKey))

    actual fun saveEntry(hashedKey: String, payload: String) {
        cache.save(ProfileScopedKey.of(hashedKey), payload)
    }

    actual fun removeEntry(hashedKey: String) {
        cache.remove(ProfileScopedKey.of(hashedKey))
    }
}
