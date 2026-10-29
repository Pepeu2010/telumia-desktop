package com.nuvio.app.features.watchprogress

import com.nuvio.app.core.storage.DesktopStorage

/** Keeps regenerable Continue Watching snapshots within the Desktop metadata allocation. */
internal class DesktopContinueWatchingSnapshotStore(
    private val store: DesktopStorage.Store,
    private val keyPrefix: String,
    private val storageKey: (String) -> String,
    private val quotaBytes: () -> Long,
    private val canWriteEntry: (Long) -> Boolean,
    private val maxBytes: Long = MAX_TOTAL_BYTES,
) {
    private val lock = Any()

    init {
        require(keyPrefix.isNotEmpty())
        require(maxBytes > 0)
    }

    fun read(key: String): String? = store.getString(storageKey(key))

    /** A denied update leaves every existing snapshot available for offline reads. */
    fun write(key: String, payload: String): Boolean = synchronized(lock) {
        if (key.isBlank() || key.length > MAX_KEY_CHARS) return@synchronized false
        val targetKey = runCatching { storageKey(key) }.getOrNull()
            ?.takeIf { it.startsWith(keyPrefix) }
            ?: return@synchronized false
        val payloadBytes = payload.toByteArray(Charsets.UTF_8).size.toLong()
        val quota = runCatching { quotaBytes().coerceAtLeast(0L) }
            .getOrDefault(0L)
            .coerceAtMost(maxBytes)
        if (payloadBytes <= 0L || payloadBytes > quota) return@synchronized false

        val entries = runCatching { store.getStringEntriesStartingWith(keyPrefix) }
            .getOrNull()
            ?: return@synchronized false
        var usedWithoutTarget = 0L
        for ((existingKey, existingPayload) in entries) {
            if (existingKey == targetKey) continue
            val size = existingPayload.toByteArray(Charsets.UTF_8).size.toLong()
            if (usedWithoutTarget > Long.MAX_VALUE - size) return@synchronized false
            usedWithoutTarget += size
        }
        if (usedWithoutTarget > quota || payloadBytes > quota - usedWithoutTarget) return@synchronized false

        val estimatedRewriteBytes = estimatedRewriteBytes(
            currentFileBytes = store.persistedFileSizeBytes(),
            key = targetKey,
            payload = payload,
        )
        if (!runCatching { canWriteEntry(estimatedRewriteBytes) }.getOrDefault(false)) return@synchronized false

        runCatching { store.putString(targetKey, payload) }.isSuccess
    }

    fun remove(key: String) {
        val targetKey = runCatching { storageKey(key) }.getOrNull()
            ?.takeIf { it.startsWith(keyPrefix) }
            ?: return
        store.remove(targetKey)
    }

    private fun estimatedRewriteBytes(currentFileBytes: Long, key: String, payload: String): Long {
        // Properties.store escapes each UTF-16 char to at most six ASCII bytes. Include the current
        // file because DesktopStorage atomically rewrites the complete preferences file.
        val valueUpperBound = payload.length.toLong().saturatingMultiply(6L)
        val keyUpperBound = key.length.toLong().saturatingMultiply(6L).saturatingAdd(128L)
        return currentFileBytes.coerceAtLeast(0L)
            .saturatingAdd(valueUpperBound)
            .saturatingAdd(keyUpperBound)
    }

    private fun Long.saturatingMultiply(other: Long): Long =
        if (this > 0 && other > Long.MAX_VALUE / this) Long.MAX_VALUE else this * other

    private fun Long.saturatingAdd(other: Long): Long =
        if (other > 0 && this > Long.MAX_VALUE - other) Long.MAX_VALUE else this + other

    companion object {
        const val MAX_TOTAL_BYTES = 8L * 1024L * 1024L
        private const val MAX_KEY_CHARS = 512
    }
}
