package com.nuvio.app.core.cache

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TimedBoundedLruCacheTest {
    @Test
    fun `least recently used fresh entry is evicted at capacity`() {
        val cache = TimedBoundedLruCache<String, Int>(capacity = 2, ttlMs = 100)
        cache.put("a", 1, nowMs = 0)
        cache.put("b", 2, nowMs = 0)

        assertEquals(1, cache.get("a", nowMs = 10))
        cache.put("c", 3, nowMs = 20)

        assertNull(cache.get("b", nowMs = 20))
        assertEquals(1, cache.get("a", nowMs = 20))
        assertEquals(3, cache.get("c", nowMs = 20))
        assertEquals(2, cache.size())
    }

    @Test
    fun `entry remains fresh through ttl boundary and expires after it`() {
        val cache = TimedBoundedLruCache<String, Int>(capacity = 2, ttlMs = 100)
        cache.put("a", 1, nowMs = 10)

        assertEquals(1, cache.get("a", nowMs = 110))
        assertNull(cache.get("a", nowMs = 111))
        assertEquals(0, cache.size())
    }

    @Test
    fun `remove and clear release retained values`() {
        val cache = TimedBoundedLruCache<String, Int>(capacity = 2, ttlMs = 100)
        cache.put("a", 1, nowMs = 0)
        cache.put("b", 2, nowMs = 0)

        assertEquals(1, cache.remove("a"))
        assertNull(cache.get("a", nowMs = 1))
        cache.clear()
        assertEquals(0, cache.size())
    }

    @Test
    fun `concurrent reads and writes stay within the configured capacity`() = runTest {
        val cache = TimedBoundedLruCache<Int, Int>(capacity = 32, ttlMs = 10_000)

        (0 until 4).map { worker ->
            async(Dispatchers.Default) {
                repeat(500) { item ->
                    val key = worker * 500 + item
                    cache.put(key, item, nowMs = key.toLong())
                    cache.get(key, nowMs = key.toLong())
                }
            }
        }.awaitAll()

        assertEquals(32, cache.size())
    }

    @Test
    fun `cache rejects nonpositive capacity and ttl`() {
        assertFailsWith<IllegalArgumentException> { TimedBoundedLruCache<String, Int>(0, 1) }
        assertFailsWith<IllegalArgumentException> { TimedBoundedLruCache<String, Int>(1, 0) }
    }
}
