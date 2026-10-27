package com.nuvio.app.features.membership

import com.nuvio.app.core.storage.DesktopMediaCache
import com.nuvio.app.core.storage.DesktopStorage
import java.io.File

internal actual object MemberAssetStorage {
    private const val accessPayloadKey = "access_payload"
    private const val backgroundCatalogPayloadKey = "profile_background_catalog_payload"

    private val store = DesktopStorage.store("nuvio_member_access")
    private val backgroundDirectory: File by lazy {
        DesktopStorage.cacheDir.resolve("member_profile_backgrounds").toFile()
    }
    private val avatarDirectory: File by lazy {
        DesktopStorage.cacheDir.resolve("member_profile_avatars").toFile()
    }

    actual fun loadAccessPayload(): String? = store.getString(accessPayloadKey)

    actual fun saveAccessPayload(payload: String) {
        store.putString(accessPayloadKey, payload)
    }

    actual fun loadProfileBackgroundCatalogPayload(): String? = store.getString(backgroundCatalogPayloadKey)

    actual fun saveProfileBackgroundCatalogPayload(payload: String) {
        store.putString(backgroundCatalogPayloadKey, payload)
    }

    actual fun loadProfileBackgroundUri(cacheKey: String): String? {
        DesktopMediaCache.memberBackgroundCache.getFile(cacheKey)?.let { return it.toURI().toString() }
        return loadLegacyProfileBackgroundUri(cacheKey)
    }

    actual fun saveProfileBackground(cacheKey: String, bytes: ByteArray): String? {
        if (bytes.isEmpty() || bytes.size.toLong() > MAX_PROFILE_BACKGROUND_BYTES) return null
        if (!DesktopMediaCache.memberBackgroundCache.put(cacheKey, bytes)) return null
        return DesktopMediaCache.memberBackgroundCache.getFile(cacheKey)?.toURI()?.toString()
    }

    actual fun profileBackgroundDeviceMemoryBytes(): Long? = DesktopMediaCache.physicalMemoryBytes()

    actual fun loadProfileAvatar(cacheKey: String): String? {
        DesktopMediaCache.memberAvatarCache.getFile(cacheKey)?.let { return it.toURI().toString() }
        val legacy = avatarFile(cacheKey)
        if (!isValidLegacyAvatar(legacy)) return null
        val bytes = runCatching { legacy.readBytes() }.getOrNull() ?: return null
        if (DesktopMediaCache.memberAvatarCache.put(cacheKey, bytes)) {
            DesktopMediaCache.memberAvatarCache.getFile(cacheKey)?.let { cached ->
                legacy.delete()
                return cached.toURI().toString()
            }
        }
        return legacy.toURI().toString()
    }

    actual fun saveProfileAvatar(cacheKey: String, bytes: ByteArray): String? {
        if (bytes.isEmpty() || bytes.size > 2 * 1024 * 1024) return null
        if (!DesktopMediaCache.memberAvatarCache.put(cacheKey, bytes)) return null
        return DesktopMediaCache.memberAvatarCache.getFile(cacheKey)?.toURI()?.toString()
    }

    actual fun clearAccess() {
        store.remove(accessPayloadKey)
    }

    private fun backgroundFile(cacheKey: String): File =
        backgroundDirectory.resolve("${safeKey(cacheKey)}.png")

    private fun avatarFile(cacheKey: String): File = avatarDirectory.resolve(safeKey(cacheKey))

    private fun isValidLegacyAvatar(file: File): Boolean = runCatching {
        val directory = avatarDirectory.toPath()
        val path = file.toPath()
        java.nio.file.Files.isRegularFile(path, java.nio.file.LinkOption.NOFOLLOW_LINKS) &&
            java.nio.file.Files.size(path) in 1..(2L * 1024 * 1024) &&
            path.toRealPath().startsWith(directory.toRealPath())
    }.getOrDefault(false)

    private fun loadLegacyProfileBackgroundUri(cacheKey: String): String? {
        val legacy = backgroundFile(cacheKey)
        if (!isValidLegacyProfileBackground(legacy)) return null
        val bytes = runCatching { legacy.readBytes() }.getOrNull() ?: return null
        if (DesktopMediaCache.memberBackgroundCache.put(cacheKey, bytes)) {
            DesktopMediaCache.memberBackgroundCache.getFile(cacheKey)?.let { migrated ->
                legacy.delete()
                return migrated.toURI().toString()
            }
        }
        return legacy.toURI().toString()
    }

    private fun isValidLegacyProfileBackground(file: File): Boolean = runCatching {
        val directory = backgroundDirectory.toPath()
        val path = file.toPath()
        !java.nio.file.Files.isSymbolicLink(directory) &&
            java.nio.file.Files.isDirectory(directory, java.nio.file.LinkOption.NOFOLLOW_LINKS) &&
            java.nio.file.Files.isRegularFile(path, java.nio.file.LinkOption.NOFOLLOW_LINKS) &&
            java.nio.file.Files.size(path) in 1..MAX_PROFILE_BACKGROUND_BYTES &&
            path.toRealPath().startsWith(directory.toRealPath())
    }.getOrDefault(false)

    private fun safeKey(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private const val MAX_PROFILE_BACKGROUND_BYTES = 8L * 1024 * 1024
}
