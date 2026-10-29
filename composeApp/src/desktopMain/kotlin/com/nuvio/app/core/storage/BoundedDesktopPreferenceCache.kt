package com.nuvio.app.core.storage

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Bounded writer for an existing Desktop preference namespace. Reads remain available at all times. */
internal class BoundedDesktopPreferenceCache(
    private val store: DesktopStorage.Store,
    private val namespacePrefix: String,
    private val entryPrefix: String,
    private val timestampPrefix: String? = null,
    private val timestampKeyFor: ((String) -> String)? = null,
    private val maxEntries: Int,
    private val quotaBytes: () -> Long,
    private val canWriteEntry: (Long) -> Boolean,
    private val savedAtMs: (String, String) -> Long,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    init {
        require(namespacePrefix.isNotEmpty() && entryPrefix.startsWith(namespacePrefix))
        require((timestampPrefix == null) == (timestampKeyFor == null))
        require(maxEntries >= 0)
    }

    fun load(key: String): String? = store.getString(key)

    fun save(key: String, value: String) {
        if (!key.startsWith(entryPrefix)) return
        val quota = quotaBytes().coerceAtLeast(0L)
        if (quota == 0L) return
        val timestamp = nowMs().coerceAtLeast(0L).toString()
        val candidateTimestampKey = timestampKeyFor?.invoke(key)
        val candidateBytes = estimateDesktopPreferenceEntryBytes(
            key to value,
            candidateTimestampKey?.let { it to timestamp },
        )
        if (candidateBytes > quota || !canWriteEntry(candidateBytes)) return

        store.updateStringEntriesStartingWith(namespacePrefix) { current ->
            val values = current.filterKeys { it.startsWith(entryPrefix) } + (key to value)
            val entries = values.map { (entryKey, entryValue) ->
                val sidecarKey = timestampKeyFor?.invoke(entryKey)
                val sidecarValue = if (entryKey == key) timestamp else sidecarKey?.let(current::get)
                val savedAt = sidecarValue?.toLongOrNull() ?: savedAtMs(entryKey, entryValue)
                BoundedPreferenceCacheEntry(
                    key = entryKey,
                    storedBytes = estimateDesktopPreferenceEntryBytes(
                        entryKey to entryValue,
                        if (sidecarValue != null && sidecarKey != null) sidecarKey to sidecarValue else null,
                    ),
                    savedAtMs = savedAt,
                )
            }
            val retained = BoundedPreferenceCachePolicy.retain(
                entries = entries,
                maxEntries = maxEntries,
                maxBytes = quota,
                protectedKey = key,
            )
            if (retained.none { it.key == key }) return@updateStringEntriesStartingWith current

            val next = current.filterKeys { existingKey ->
                !existingKey.startsWith(entryPrefix) &&
                    (timestampPrefix == null || !existingKey.startsWith(timestampPrefix))
            }.toMutableMap()
            retained.forEach { entry ->
                val retainedValue = values[entry.key] ?: return@forEach
                next[entry.key] = retainedValue
                val sidecarKey = timestampKeyFor?.invoke(entry.key)
                val sidecarValue = if (entry.key == key) timestamp else sidecarKey?.let(current::get)
                if (sidecarKey != null && sidecarValue != null) next[sidecarKey] = sidecarValue
            }
            next
        }
    }

    fun remove(key: String) {
        if (!key.startsWith(entryPrefix)) return
        val timestampKey = timestampKeyFor?.invoke(key)
        store.updateStringEntriesStartingWith(namespacePrefix) { current ->
            current - key - listOfNotNull(timestampKey)
        }
    }
}

internal fun estimateDesktopPreferenceEntryBytes(
    entry: Pair<String, String>,
    sidecar: Pair<String, String>? = null,
): Long {
    var total = estimatePropertiesEntryBytes(entry.first, entry.second)
    if (sidecar != null) total = saturatedAdd(total, estimatePropertiesEntryBytes(sidecar.first, sidecar.second))
    return total
}

private fun estimatePropertiesEntryBytes(key: String, value: String): Long =
    saturatedAdd(saturatedAdd(estimatedPropertyChars(key), estimatedPropertyChars(value)), 128L)

private fun estimatedPropertyChars(value: String): Long {
    var total = 0L
    value.forEach { char ->
        val encodedChars = when {
            char < ' ' || char > '~' -> 6L
            char == '\\' || char == '\t' || char == '\n' || char == '\r' || char == '\u000c' ||
                char == '#' || char == '!' || char == '=' || char == ':' -> 2L
            else -> 1L
        }
        total = saturatedAdd(total, encodedChars)
    }
    return total
}

private fun saturatedAdd(left: Long, right: Long): Long =
    if (left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right

internal object StreamLinkCacheTimestamp {
    private val json = Json { ignoreUnknownKeys = true }

    fun read(payload: String): Long = runCatching {
        json.parseToJsonElement(payload).jsonObject["cachedAtMs"]?.jsonPrimitive?.longOrNull
    }.getOrNull() ?: 0L
}
