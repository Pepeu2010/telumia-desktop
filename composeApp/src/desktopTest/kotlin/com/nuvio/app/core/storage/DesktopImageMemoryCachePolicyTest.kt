package com.nuvio.app.core.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopImageMemoryCachePolicyTest {
    private val mib = 1024L * 1024L

    @Test
    fun unknownOrInvalidMemoryUsesSmallFallback() {
        val expected = 24L * mib
        assertEquals(expected, DesktopImageMemoryCachePolicy.allocate(null).totalBytes)
        assertEquals(expected, DesktopImageMemoryCachePolicy.allocate(0L).totalBytes)
        assertEquals(expected, DesktopImageMemoryCachePolicy.allocate(-1L).totalBytes)
    }

    @Test
    fun scalesWithMemoryBetweenTheConfiguredBounds() {
        assertEquals(32L * mib, DesktopImageMemoryCachePolicy.allocate(3_200L * mib).totalBytes)
        assertEquals(64L * mib, DesktopImageMemoryCachePolicy.allocate(6_400L * mib).totalBytes)
    }

    @Test
    fun clampsLowAndHighMemory() {
        assertEquals(16L * mib, DesktopImageMemoryCachePolicy.allocate(512L * mib).totalBytes)
        assertEquals(96L * mib, DesktopImageMemoryCachePolicy.allocate(32L * 1024L * mib).totalBytes)
    }

    @Test
    fun splitsTheTotalAcrossImageAndBadgeLoaders() {
        val allocation = DesktopImageMemoryCachePolicy.allocate(8L * 1024L * mib)

        assertEquals(allocation.totalBytes, allocation.imagesBytes + allocation.badgesBytes)
        assertEquals(allocation.totalBytes / 8L, allocation.badgesBytes)
        assertTrue(allocation.imagesBytes > allocation.badgesBytes)
    }
}
