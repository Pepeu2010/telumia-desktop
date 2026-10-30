package com.nuvio.app.features.trakt

import kotlin.test.Test
import kotlin.test.assertEquals

class TraktEpisodeMappingCacheLimitsTest {
    @Test
    fun `unknown and low memory use conservative limits`() {
        val expected = TraktEpisodeMappingCacheLimits(96, 4, 4, 96, 512)

        assertEquals(expected, TraktEpisodeMappingCacheLimits.forPhysicalMemoryBytes(null))
        assertEquals(expected, TraktEpisodeMappingCacheLimits.forPhysicalMemoryBytes(0L))
        assertEquals(expected, TraktEpisodeMappingCacheLimits.forPhysicalMemoryBytes(2L * GIB))
    }

    @Test
    fun `balanced memory uses the middle limits`() {
        assertEquals(
            TraktEpisodeMappingCacheLimits(192, 8, 8, 192, 1_024),
            TraktEpisodeMappingCacheLimits.forPhysicalMemoryBytes(2L * GIB + 1L),
        )
        assertEquals(
            TraktEpisodeMappingCacheLimits(192, 8, 8, 192, 1_024),
            TraktEpisodeMappingCacheLimits.forPhysicalMemoryBytes(3L * GIB),
        )
    }

    @Test
    fun `high capacity memory uses larger limits`() {
        assertEquals(
            TraktEpisodeMappingCacheLimits(384, 16, 16, 384, 2_048),
            TraktEpisodeMappingCacheLimits.forPhysicalMemoryBytes(3L * GIB + 1L),
        )
    }

    private companion object {
        const val GIB = 1024L * 1024L * 1024L
    }
}
