package com.nuvio.app.features.trakt

import kotlin.test.Test
import kotlin.test.assertEquals

class TraktCommentsCacheLimitsTest {
    @Test
    fun unknownAndLowMemoryUseEightTitlesAndTwoPagesPerTitle() {
        val expected = TraktCommentsCacheLimits(titleEntries = 8, pagesPerTitle = 2)

        assertEquals(expected, TraktCommentsCacheLimits.forPhysicalMemoryBytes(null))
        assertEquals(expected, TraktCommentsCacheLimits.forPhysicalMemoryBytes(0L))
        assertEquals(expected, TraktCommentsCacheLimits.forPhysicalMemoryBytes(4L * GIB))
    }

    @Test
    fun balancedMemoryAllowsSixteenTitlesAndThreePagesPerTitle() {
        val expected = TraktCommentsCacheLimits(titleEntries = 16, pagesPerTitle = 3)

        assertEquals(expected, TraktCommentsCacheLimits.forPhysicalMemoryBytes(4L * GIB + 1L))
        assertEquals(expected, TraktCommentsCacheLimits.forPhysicalMemoryBytes(8L * GIB))
    }

    @Test
    fun highMemoryAllowsTwentyFourTitlesAndFourPagesPerTitle() {
        assertEquals(
            TraktCommentsCacheLimits(titleEntries = 24, pagesPerTitle = 4),
            TraktCommentsCacheLimits.forPhysicalMemoryBytes(8L * GIB + 1L),
        )
    }

    private companion object {
        const val GIB = 1024L * 1024L * 1024L
    }
}
