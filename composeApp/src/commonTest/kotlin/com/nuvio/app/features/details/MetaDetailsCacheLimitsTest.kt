package com.nuvio.app.features.details

import kotlin.test.Test
import kotlin.test.assertEquals

class MetaDetailsCacheLimitsTest {
    @Test
    fun `unknown and low memory use the conservative initial limit`() {
        val expected = MetaDetailsCacheLimits(entries = 8)

        assertEquals(expected, MetaDetailsCacheLimits.forPhysicalMemoryBytes(null))
        assertEquals(expected, MetaDetailsCacheLimits.forPhysicalMemoryBytes(0L))
        assertEquals(expected, MetaDetailsCacheLimits.forPhysicalMemoryBytes(4L * GIB))
    }

    @Test
    fun `balanced memory allows sixteen retained metadata entries`() {
        assertEquals(
            MetaDetailsCacheLimits(entries = 16),
            MetaDetailsCacheLimits.forPhysicalMemoryBytes(4L * GIB + 1L),
        )
        assertEquals(
            MetaDetailsCacheLimits(entries = 16),
            MetaDetailsCacheLimits.forPhysicalMemoryBytes(8L * GIB),
        )
    }

    @Test
    fun `high capacity memory allows twenty four retained metadata entries`() {
        assertEquals(
            MetaDetailsCacheLimits(entries = 24),
            MetaDetailsCacheLimits.forPhysicalMemoryBytes(8L * GIB + 1L),
        )
    }

    private companion object {
        const val GIB = 1024L * 1024L * 1024L
    }
}
