package com.nuvio.app.features.tracking

import com.nuvio.app.core.storage.DesktopStorage
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object TrackingSettingsStorage {
    private val store = DesktopStorage.store("nuvio_trakt_settings")
    private const val pendingWatchProgressSourceKey = "pending_watch_progress_source"

    actual fun loadPayload(): String? =
        store.getString(ProfileScopedKey.of("trakt_settings"))

    actual fun savePayload(payload: String) {
        store.putString(ProfileScopedKey.of("trakt_settings"), payload)
    }

    actual fun loadPendingWatchProgressSourcePayload(profileId: Int): String? =
        store.getString(ProfileScopedKey.of(pendingWatchProgressSourceKey, profileId))

    actual fun savePendingWatchProgressSourcePayload(profileId: Int, payload: String) {
        store.putString(ProfileScopedKey.of(pendingWatchProgressSourceKey, profileId), payload)
    }

    actual fun clearPendingWatchProgressSourcePayload(profileId: Int) {
        store.remove(ProfileScopedKey.of(pendingWatchProgressSourceKey, profileId))
    }
}
