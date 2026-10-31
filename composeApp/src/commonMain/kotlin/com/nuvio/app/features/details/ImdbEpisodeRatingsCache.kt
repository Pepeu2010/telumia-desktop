package com.nuvio.app.features.details

import com.nuvio.app.core.cache.SynchronizedBoundedLruCache

/** Bounded in-memory cache for the per-title episode ratings shown on details pages. */
internal class ImdbEpisodeRatingsCache(
    capacity: Int,
    private val ttlMs: Long,
) {
    private data class Entry(
        val ratings: Map<Pair<Int, Int>, Double>,
        val expiresAtMs: Long,
    )

    private val entries = SynchronizedBoundedLruCache<String, Entry>(capacity)

    init {
        require(ttlMs > 0L) { "Episode ratings cache TTL must be positive" }
    }

    fun get(key: String, nowMs: Long): Map<Pair<Int, Int>, Double>? {
        val entry = entries.get(key) ?: return null
        if (entry.expiresAtMs <= nowMs) {
            entries.remove(key)
            return null
        }
        return entry.ratings
    }

    fun put(key: String, ratings: Map<Pair<Int, Int>, Double>, nowMs: Long) {
        entries.put(key, Entry(ratings, nowMs + ttlMs))
    }

    fun clear() = entries.clear()

    fun size(): Int = entries.size()
}
