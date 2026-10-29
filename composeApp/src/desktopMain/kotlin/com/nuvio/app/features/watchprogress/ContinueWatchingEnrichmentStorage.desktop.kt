package com.nuvio.app.features.watchprogress

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.DesktopMediaCache
import com.nuvio.app.core.storage.MediaCacheCategory
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object ContinueWatchingEnrichmentStorage {
    private val store = DesktopStorage.store("nuvio_continue_watching_enrichment")
    private const val keyPrefix = "continue_watching_enrichment_"
    private val snapshots by lazy {
        DesktopContinueWatchingSnapshotStore(
            store = store,
            keyPrefix = keyPrefix,
            storageKey = ::scopedKey,
            quotaBytes = {
                DesktopMediaCache.activeBudget.quota(MediaCacheCategory.METADATA)
                    .coerceAtMost(DesktopContinueWatchingSnapshotStore.MAX_TOTAL_BYTES)
            },
            canWriteEntry = { bytes ->
                DesktopMediaCache.canWriteCacheEntry(
                    MediaCacheCategory.METADATA,
                    bytes,
                    DesktopStorage.rootDir,
                )
            },
        )
    }

    actual fun loadPayload(key: String): String? = snapshots.read(key)

    actual fun savePayload(key: String, payload: String): Boolean = snapshots.write(key, payload)

    actual fun removePayload(key: String) = snapshots.remove(key)

    private fun scopedKey(key: String): String = ProfileScopedKey.of("$keyPrefix$key")
}
