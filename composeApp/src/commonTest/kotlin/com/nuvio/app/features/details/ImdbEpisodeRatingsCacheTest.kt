package com.nuvio.app.features.details

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImdbEpisodeRatingsCacheTest {
    @Test
    fun keepsRecentTitleRatingsAndEvictsLeastRecentlyUsedEntryAtCapacity() {
        val cache = ImdbEpisodeRatingsCache(capacity = 2, ttlMs = 100)
        val first = mapOf((1 to 1) to 7.5)
        val second = mapOf((1 to 2) to 8.0)
        val third = mapOf((1 to 3) to 8.5)

        cache.put("tt0000001", first, nowMs = 0)
        cache.put("tt0000002", second, nowMs = 0)
        assertEquals(first, cache.get("tt0000001", nowMs = 1))
        cache.put("tt0000003", third, nowMs = 2)

        assertEquals(first, cache.get("tt0000001", nowMs = 3))
        assertNull(cache.get("tt0000002", nowMs = 3))
        assertEquals(third, cache.get("tt0000003", nowMs = 3))
        assertEquals(2, cache.size())
    }

    @Test
    fun expiresRatingsAtTheEndOfTheirTtlAndRemovesThemFromMemory() {
        val cache = ImdbEpisodeRatingsCache(capacity = 2, ttlMs = 10)
        val ratings = mapOf((2 to 4) to 9.0)
        cache.put("tt0000004", ratings, nowMs = 100)

        assertEquals(ratings, cache.get("tt0000004", nowMs = 109))
        assertNull(cache.get("tt0000004", nowMs = 110))
        assertEquals(0, cache.size())
    }

    @Test
    fun cachesEmptyResponsesAndSupportsClearing() {
        val cache = ImdbEpisodeRatingsCache(capacity = 1, ttlMs = 10)
        cache.put("tt0000005", emptyMap(), nowMs = 0)

        assertEquals(emptyMap(), cache.get("tt0000005", nowMs = 1))
        assertTrue(cache.size() == 1)
        cache.clear()
        assertNull(cache.get("tt0000005", nowMs = 1))
        assertEquals(0, cache.size())
    }
}
