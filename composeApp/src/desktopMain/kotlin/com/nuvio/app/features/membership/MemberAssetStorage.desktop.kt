package com.nuvio.app.features.membership

import com.nuvio.app.core.storage.DesktopMediaCache
import com.nuvio.app.core.storage.DesktopStorage

internal actual object MemberAssetStorage {
    private const val accessPayloadKey = "access_payload"
    private const val backgroundCatalogPayloadKey = "profile_background_catalog_payload"

    private val store = DesktopStorage.store("nuvio_member_access")

    actual fun loadAccessPayload(): String? = store.getString(accessPayloadKey)

    actual fun saveAccessPayload(payload: String) {
        store.putString(accessPayloadKey, payload)
    }

    actual fun loadProfileBackgroundCatalogPayload(): String? = store.getString(backgroundCatalogPayloadKey)

    actual fun saveProfileBackgroundCatalogPayload(payload: String) {
        store.putString(backgroundCatalogPayloadKey, payload)
    }

    actual fun loadProfileBackgroundUri(cacheKey: String): String? {
        return DesktopMediaCache.memberBackgroundCache.getFile(cacheKey)?.toURI()?.toString()
    }

    actual fun saveProfileBackground(cacheKey: String, bytes: ByteArray): String? {
        return DesktopMediaCache.memberBackgroundCache.put(cacheKey, bytes)?.toURI()?.toString()
    }

    actual fun profileBackgroundDeviceMemoryBytes(): Long? = DesktopMediaCache.physicalMemoryBytes()

    actual fun loadProfileAvatar(cacheKey: String): String? =
        DesktopMediaCache.memberAvatarCache.getFile(cacheKey)?.toURI()?.toString()

    actual fun saveProfileAvatar(cacheKey: String, bytes: ByteArray): String? =
        DesktopMediaCache.memberAvatarCache.put(cacheKey, bytes)?.toURI()?.toString()

    actual fun clearAccess() {
        store.remove(accessPayloadKey)
    }

}
