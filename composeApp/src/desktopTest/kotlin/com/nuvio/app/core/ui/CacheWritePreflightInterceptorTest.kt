package com.nuvio.app.core.ui

import coil3.request.CachePolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class CacheWritePreflightInterceptorTest {
    @Test fun lowStorageKeepsDiskReadsAndTurnsOffWrites() {
        assertEquals(CachePolicy.READ_ONLY, cachePolicyAfterStorageCheck(CachePolicy.ENABLED, canWrite = false))
    }

    @Test fun lowStoragePreservesAnExplicitReadOnlyRequest() {
        assertEquals(CachePolicy.READ_ONLY, cachePolicyAfterStorageCheck(CachePolicy.READ_ONLY, canWrite = false))
    }

    @Test fun lowStoragePreservesAnExplicitlyDisabledRequest() {
        assertEquals(CachePolicy.DISABLED, cachePolicyAfterStorageCheck(CachePolicy.DISABLED, canWrite = false))
    }

    @Test fun lowStorageBlocksWriteOnlyRequestsWithoutEnablingReads() {
        assertEquals(CachePolicy.DISABLED, cachePolicyAfterStorageCheck(CachePolicy.WRITE_ONLY, canWrite = false))
    }

    @Test fun availableStoragePreservesEveryRequestPolicy() {
        for (policy in CachePolicy.entries) {
            assertEquals(policy, cachePolicyAfterStorageCheck(policy, canWrite = true))
        }
    }

    @Test fun onlyRequestsThatStillAllowDiskWritesNeedAStorageProbe() {
        assertTrue(CachePolicy.ENABLED.writeEnabled)
        assertTrue(CachePolicy.WRITE_ONLY.writeEnabled)
        assertFalse(CachePolicy.READ_ONLY.writeEnabled)
        assertFalse(CachePolicy.DISABLED.writeEnabled)
    }

    @Test fun storageProbeIsSharedForOneSecondAndFailsClosedWhenSamplingThrows() = runTest {
        var now = 10L
        var allowed = true
        var samples = 0
        val sampler = StorageWritePreflightSampler(
            sample = { samples++; if (allowed) true else error("storage probe failed") },
            clockMillis = { now },
        )

        assertEquals(true, sampler.canWrite())
        assertEquals(true, sampler.canWrite())
        assertEquals(1, samples)
        now += 1_000L
        allowed = false
        assertEquals(false, sampler.canWrite())
        assertEquals(false, sampler.canWrite())
        assertEquals(2, samples)
    }
}
