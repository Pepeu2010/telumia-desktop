package com.nuvio.app.features.details

import kotlin.test.Test
import kotlin.test.assertEquals

class RelatedMetadataCacheLimitsTest {
    @Test
    fun `unknown and low memory use the conservative initial limit`() {
        val expected = RelatedMetadataCacheLimits(entries = 12)

        assertEquals(expected, RelatedMetadataCacheLimits.forPhysicalMemoryBytes(null))
        assertEquals(expected, RelatedMetadataCacheLimits.forPhysicalMemoryBytes(0L))
        assertEquals(expected, RelatedMetadataCacheLimits.forPhysicalMemoryBytes(4L * GIB))
    }

    @Test
    fun `balanced memory allows twenty four retained responses`() {
        assertEquals(
            RelatedMetadataCacheLimits(entries = 24),
            RelatedMetadataCacheLimits.forPhysicalMemoryBytes(4L * GIB + 1L),
        )
        assertEquals(
            RelatedMetadataCacheLimits(entries = 24),
            RelatedMetadataCacheLimits.forPhysicalMemoryBytes(8L * GIB),
        )
    }

    @Test
    fun `high capacity memory allows forty eight retained responses`() {
        assertEquals(
            RelatedMetadataCacheLimits(entries = 48),
            RelatedMetadataCacheLimits.forPhysicalMemoryBytes(8L * GIB + 1L),
        )
    }

    private companion object {
        const val GIB = 1024L * 1024L * 1024L
    }
}
