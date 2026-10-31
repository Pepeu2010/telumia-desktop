package com.nuvio.app.features.details

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MetaDetailsRepositoryCacheTest {
    @Test
    fun `cache hit updates recency before least recently used entry is evicted`() {
        val cache = BoundedLruCache<Int, String>(maxEntries = 2)
        cache[1] = "one"
        cache[2] = "two"

        assertEquals("one", cache[1])

        cache[3] = "three"

        assertEquals("one", cache[1])
        assertNull(cache[2])
        assertEquals("three", cache[3])
    }

    @Test
    fun `repository cache evicts the least recently accessed key at its 128 entry limit`() {
        val cache = BoundedLruCache<Int, String>(maxEntries = 128)
        repeat(128) { key -> cache[key] = "meta-$key" }

        assertEquals("meta-0", cache[0])
        cache[128] = "meta-128"

        assertEquals(128, cache.size())
        assertEquals("meta-0", cache[0])
        assertNull(cache[1])
        assertEquals("meta-128", cache[128])
    }

    @Test
    fun `clear removes all cached entries`() {
        val cache = BoundedLruCache<String, String>(maxEntries = 2)
        cache["movie:1"] = "one"
        cache["series:2"] = "two"

        cache.clear()

        assertEquals(0, cache.size())
        assertNull(cache["movie:1"])
        assertNull(cache["series:2"])
    }

    @Test
    fun `missing or failed fetch result does not populate cache`() {
        val cache = BoundedLruCache<String, String>(maxEntries = 2)
        val fetchResult: String? = null

        cache.putIfPresent("series:missing", fetchResult)

        assertNull(cache["series:missing"])
    }
}
