package com.nuvio.app.core.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopMetadataPreferenceCachePolicyTest {
    @Test
    fun `metadata preference shares shrink below the Continue Watching reservation`() {
        val allocation = DesktopMetadataPreferenceCachePolicy.allocate(8L * MIB)

        assertEquals(0L, allocation.streamLinksBytes)
        assertEquals(0L, allocation.bingeGroupsBytes)
    }

    @Test
    fun `metadata preference shares remain inside the adaptive category quota`() {
        val quota = 24L * MIB
        val allocation = DesktopMetadataPreferenceCachePolicy.allocate(quota)

        assertEquals(8L * MIB, allocation.streamLinksBytes)
        assertEquals(2L * MIB, allocation.bingeGroupsBytes)
        assertTrue(allocation.streamLinksBytes + allocation.bingeGroupsBytes <= quota - 8L * MIB)
    }
}
