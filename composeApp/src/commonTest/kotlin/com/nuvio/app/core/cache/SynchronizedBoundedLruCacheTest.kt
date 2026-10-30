package com.nuvio.app.core.cache

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SynchronizedBoundedLruCacheTest {
    @Test
    fun `least recently used entry is evicted after a cache hit`() {
        val cache = SynchronizedBoundedLruCache<String, Int>(2)
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
    fun `replacement updates value and recency`() {
        val cache = SynchronizedBoundedLruCache<String, Int>(2)
        cache.put("a", 1)
        cache.put("b", 2)
        cache.put("a", 3)
        cache.put("c", 4)

        assertEquals(3, cache.get("a"))
        assertNull(cache.get("b"))
        assertEquals(2, cache.size())
    }

    @Test
    fun `concurrent writes never exceed the configured capacity`() = runTest {
        val cache = SynchronizedBoundedLruCache<Int, Int>(16)

        (0 until 4).map { worker ->
            async(Dispatchers.Default) {
                repeat(500) { item -> cache.put(worker * 500 + item, item) }
            }
        }.awaitAll()

        assertEquals(16, cache.size())
    }

    @Test
    fun `clear removes every retained value`() {
        val cache = SynchronizedBoundedLruCache<String, Int>(2)
        cache.put("a", 1)
        cache.put("b", 2)

        cache.clear()

        assertEquals(0, cache.size())
    }

    @Test
    fun `cache rejects a nonpositive capacity`() {
        assertFailsWith<IllegalArgumentException> { SynchronizedBoundedLruCache<String, Int>(0) }
    }
}
