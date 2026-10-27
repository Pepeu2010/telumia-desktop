package com.nuvio.app.features.membership

internal const val PROFILE_BACKGROUND_MIB: Long = 1024L * 1024L

/** A small, byte-counted fallback for devices where remote images cannot be persisted to disk. */
internal object ProfileBackgroundMemoryPolicy {
    private const val LOW_RAM_BYTES = 1536L * PROFILE_BACKGROUND_MIB
    private const val BALANCED_RAM_BYTES = 4096L * PROFILE_BACKGROUND_MIB

    fun maxRetainedBytes(totalMemoryBytes: Long?): Long = when {
        totalMemoryBytes == null || totalMemoryBytes <= 0L || totalMemoryBytes <= LOW_RAM_BYTES ->
            8L * PROFILE_BACKGROUND_MIB
        totalMemoryBytes <= BALANCED_RAM_BYTES -> 24L * PROFILE_BACKGROUND_MIB
        else -> 48L * PROFILE_BACKGROUND_MIB
    }
}

internal data class ProfileBackgroundMemoryKey(
    val id: String,
    val version: Int,
    val portrait: Boolean,
)

/** Tracks only fallback ByteArrays; disk-backed images consume no budget here. */
internal class ProfileBackgroundMemoryBudget(maxBytes: Long) {
    private val maxBytes = maxBytes.coerceAtLeast(0L)
    private val accessOrder = linkedMapOf<ProfileBackgroundMemoryKey, Long>()
    var retainedBytes: Long = 0L
        private set

    fun record(key: ProfileBackgroundMemoryKey, sizeBytes: Long): List<ProfileBackgroundMemoryKey> {
        remove(key)
        if (sizeBytes <= 0L) return emptyList()
        if (sizeBytes > maxBytes) return listOf(key)
        accessOrder[key] = sizeBytes
        retainedBytes += sizeBytes
        val evicted = mutableListOf<ProfileBackgroundMemoryKey>()
        while (retainedBytes > maxBytes) {
            val oldest = accessOrder.entries.firstOrNull() ?: break
            accessOrder.remove(oldest.key)
            retainedBytes = (retainedBytes - oldest.value).coerceAtLeast(0L)
            evicted += oldest.key
        }
        return evicted
    }

    fun touch(key: ProfileBackgroundMemoryKey) {
        accessOrder.remove(key)?.let { accessOrder[key] = it }
    }

    fun remove(key: ProfileBackgroundMemoryKey): Boolean {
        val removed = accessOrder.remove(key) ?: return false
        retainedBytes = (retainedBytes - removed).coerceAtLeast(0L)
        return true
    }

    fun clear() {
        accessOrder.clear()
        retainedBytes = 0L
    }
}
