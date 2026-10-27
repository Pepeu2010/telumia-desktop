package com.nuvio.app.features.membership

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileBackgroundMemoryPolicyTest {
    @Test fun fallbackMemoryBudgetFollowsDeviceMemoryTier() {
        assertEquals(8L * PROFILE_BACKGROUND_MIB, ProfileBackgroundMemoryPolicy.maxRetainedBytes(null))
        assertEquals(8L * PROFILE_BACKGROUND_MIB, ProfileBackgroundMemoryPolicy.maxRetainedBytes(1024L * PROFILE_BACKGROUND_MIB))
        assertEquals(24L * PROFILE_BACKGROUND_MIB, ProfileBackgroundMemoryPolicy.maxRetainedBytes(3072L * PROFILE_BACKGROUND_MIB))
        assertEquals(48L * PROFILE_BACKGROUND_MIB, ProfileBackgroundMemoryPolicy.maxRetainedBytes(8192L * PROFILE_BACKGROUND_MIB))
    }

    @Test fun fallbackImagesEvictLeastRecentlyUsedAndRejectOversizedEntries() {
        val budget = ProfileBackgroundMemoryBudget(maxBytes = 6)
        val landscapeA = ProfileBackgroundMemoryKey("a", 1, portrait = false)
        val landscapeB = ProfileBackgroundMemoryKey("b", 1, portrait = false)
        val landscapeC = ProfileBackgroundMemoryKey("c", 1, portrait = false)
        assertEquals(emptyList(), budget.record(landscapeA, 3))
        assertEquals(emptyList(), budget.record(landscapeB, 3))
        budget.touch(landscapeA)
        assertEquals(listOf(landscapeB), budget.record(landscapeC, 3))
        assertEquals(6L, budget.retainedBytes)
        assertEquals(listOf(landscapeB), budget.record(landscapeB, 7))
        assertEquals(6L, budget.retainedBytes)
    }
}
