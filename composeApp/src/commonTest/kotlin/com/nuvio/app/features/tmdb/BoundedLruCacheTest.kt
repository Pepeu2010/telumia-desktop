package com.nuvio.app.features.tmdb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BoundedLruCacheTest {
    @Test
    fun `least recently used entry is evicted after a cache hit`() = runTest {
        val cache = BoundedLruCache<String, Int>(2)
        cache.put("a", 1)
        cache.put("b", 2)

        assertEquals(1, cache.get("a"))
        cache.put("c", 3)

        assertNull(cache.get("b"))
        assertEquals(1, cache.get("a"))
        assertEquals(3, cache.get("c"))
        assertEquals(2, cache.size())
    }

    @Test
    fun `replacing a key keeps one entry and makes it most recent`() = runTest {
        val cache = BoundedLruCache<String, Int>(2)
        cache.put("a", 1)
        cache.put("b", 2)
        cache.put("a", 3)
        cache.put("c", 4)

        assertEquals(3, cache.get("a"))
        assertNull(cache.get("b"))
        assertEquals(2, cache.size())
    }

    @Test
    fun `concurrent writes keep the configured bound`() = runTest {
        val cache = BoundedLruCache<Int, Int>(16)
        (0 until 4).map { worker ->
            async(Dispatchers.Default) {
                repeat(500) { item -> cache.put(worker * 500 + item, item) }
            }
        }.awaitAll()

        assertEquals(16, cache.size())
    }

    @Test
    fun `metadata cache defaults stay positive and bounded`() {
        val values = TmdbMetadataCacheLimits.DEFAULT
        val limits = listOf(
            values.enrichment, values.episodes, values.recommendations, values.collections,
            values.trailers, values.people, values.entityBrowse, values.entityHeaders, values.entityRails,
        )
        assertEquals(9, limits.size)
        assertTrue(limits.all { it > 0 })
        assertEquals(24, values.enrichment)
        assertEquals(16, values.episodes)
        assertEquals(24, values.people)
        assertEquals(12, values.recommendations)
    }

    @Test
    fun `cache rejects a nonpositive capacity`() {
        assertFailsWith<IllegalArgumentException> { BoundedLruCache<String, Int>(0) }
    }
}
