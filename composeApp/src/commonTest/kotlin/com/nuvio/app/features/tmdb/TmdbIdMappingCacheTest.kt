package com.nuvio.app.features.tmdb

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TmdbIdMappingCacheTest {
    @Test
    fun `both mapping directions evict the least recently used key`() = runTest {
        val cache = TmdbIdMappingCache(capacityPerDirection = 2)
        cache.remember("tt0000001", "1", "movie")
        cache.remember("tt0000002", "2", "movie")

        assertEquals("1", cache.tmdbIdForImdb("tt0000001", "film"))
        assertEquals("tt0000001", cache.imdbIdForTmdb("1", "movie"))

        cache.remember("tt0000003", "3", "movie")

        assertNull(cache.tmdbIdForImdb("tt0000002", "movie"))
        assertNull(cache.imdbIdForTmdb("2", "film"))
        assertEquals("1", cache.tmdbIdForImdb("tt0000001", "movie"))
        assertEquals("tt0000001", cache.imdbIdForTmdb("1", "film"))
    }

    @Test
    fun `mapping keys remain scoped by normalized media type`() = runTest {
        val cache = TmdbIdMappingCache(capacityPerDirection = 4)
        cache.remember("tt0000001", "1", "movie")
        cache.remember("tt0000001", "2", "series")

        assertEquals("1", cache.tmdbIdForImdb("tt0000001", "film"))
        assertEquals("2", cache.tmdbIdForImdb("tt0000001", "tv"))
    }
}
