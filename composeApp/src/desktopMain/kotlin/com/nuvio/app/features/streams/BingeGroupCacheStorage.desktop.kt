package com.nuvio.app.features.streams

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.DesktopMediaCache
import com.nuvio.app.core.storage.BoundedDesktopPreferenceCache
import com.nuvio.app.core.storage.DesktopMetadataPreferenceCachePolicy
import com.nuvio.app.core.storage.MediaCacheCategory
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object BingeGroupCacheStorage {
    private val store = DesktopStorage.store("nuvio_binge_group_cache")
    private val cache by lazy {
        BoundedDesktopPreferenceCache(
            store = store,
            namespacePrefix = "binge_",
            entryPrefix = "binge_group_",
            timestampPrefix = "binge_saved_at_",
            timestampKeyFor = { key -> key.replaceFirst("binge_group_", "binge_saved_at_") },
            maxEntries = 1_024,
            quotaBytes = {
                if (!DesktopMediaCache.writesEnabled) 0L
                else DesktopMetadataPreferenceCachePolicy.allocate(
                    DesktopMediaCache.cacheQuota(MediaCacheCategory.METADATA),
                ).bingeGroupsBytes
            },
            canWriteEntry = { bytes ->
                DesktopMediaCache.canWriteCacheEntry(
                    MediaCacheCategory.METADATA,
                    bytes,
                    DesktopStorage.rootDir,
                )
            },
            savedAtMs = { _, _ -> 0L },
        )
    }

    actual fun load(hashedKey: String): String? =
        cache.load(ProfileScopedKey.of(hashedKey))

    actual fun save(hashedKey: String, value: String) {
        cache.save(ProfileScopedKey.of(hashedKey), value)
    }

    actual fun remove(hashedKey: String) {
        cache.remove(ProfileScopedKey.of(hashedKey))
    }
}
