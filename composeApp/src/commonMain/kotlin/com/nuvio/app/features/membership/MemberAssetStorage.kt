package com.nuvio.app.features.membership

internal expect object MemberAssetStorage {
    fun loadAccessPayload(): String?
    fun saveAccessPayload(payload: String)
    fun loadProfileBackgroundCatalogPayload(): String?
    fun saveProfileBackgroundCatalogPayload(payload: String)
    fun loadProfileBackgroundUri(cacheKey: String): String?
    fun saveProfileBackground(cacheKey: String, bytes: ByteArray): String?
    fun profileBackgroundDeviceMemoryBytes(): Long?
    fun loadProfileAvatar(cacheKey: String): String?
    fun saveProfileAvatar(cacheKey: String, bytes: ByteArray): String?
    fun clearAccess()
}
