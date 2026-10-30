package com.nuvio.app.features.trakt

import com.nuvio.app.features.tmdb.BoundedLruCache
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val NON_ALPHANUMERIC = Regex("[^a-z0-9]+")
private val COLLAPSED_SPACES = Regex("\\s+")

/** Keeps title matching unchanged while bounding normalized titles retained in memory. */
internal class EpisodeTitleNormalizer(capacity: Int) {
    private val mutex = Mutex()
    private val normalizedTitles = BoundedLruCache<String, String>(capacity)

    suspend fun normalize(title: String?): String {
        if (title == null) return ""
        return mutex.withLock {
            normalizedTitles.get(title) ?: normalizeEpisodeTitle(title).also { normalizedTitles.put(title, it) }
        }
    }

    suspend fun clear() = mutex.withLock { normalizedTitles.clear() }

    suspend fun size(): Int = normalizedTitles.size()
}

private fun normalizeEpisodeTitle(title: String): String = title.lowercase()
    .replace(NON_ALPHANUMERIC, " ")
    .trim()
    .replace(COLLAPSED_SPACES, " ")
