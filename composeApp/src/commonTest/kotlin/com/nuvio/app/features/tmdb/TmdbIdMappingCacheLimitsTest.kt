package com.nuvio.app.features.tmdb

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TmdbIdMappingCacheLimitsTest {
    @Test
    fun `unknown and low memory use the conservative id mapping limit`() {
        val twoGiB = 2L * GIB

        assertEquals(128, TmdbIdMappingCacheLimits.forPhysicalMemoryBytes(null).entriesPerDirection)
        assertEquals(128, TmdbIdMappingCacheLimits.forPhysicalMemoryBytes(0L).entriesPerDirection)
        assertEquals(128, TmdbIdMappingCacheLimits.forPhysicalMemoryBytes(twoGiB).entriesPerDirection)
    }

    @Test
    fun `balanced and high memory select larger id mapping limits`() {
        val twoGiB = 2L * GIB
        val threeGiB = 3L * GIB

        assertEquals(256, TmdbIdMappingCacheLimits.forPhysicalMemoryBytes(twoGiB + 1L).entriesPerDirection)
        assertEquals(256, TmdbIdMappingCacheLimits.forPhysicalMemoryBytes(threeGiB).entriesPerDirection)
        assertEquals(512, TmdbIdMappingCacheLimits.forPhysicalMemoryBytes(threeGiB + 1L).entriesPerDirection)
    }

    @Test
    fun `cache capacity must be positive`() {
        assertFailsWith<IllegalArgumentException> { TmdbIdMappingCacheLimits(0) }
    }

    private companion object {
        const val GIB = 1024L * 1024L * 1024L
    }
}
