package com.nuvio.app.features.settings

import com.nuvio.app.features.simkl.SimklConnectionMode
import com.nuvio.app.features.tracking.MoreLikeThisSourcePreference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackingSettingsPresentationTest {
    @Test
    fun `provider connection modes map to matching card modes`() {
        assertEquals(
            TrackingConnectionCardMode.DISCONNECTED,
            SimklConnectionMode.DISCONNECTED.toTrackingConnectionCardMode(),
        )
        assertEquals(
            TrackingConnectionCardMode.AWAITING_APPROVAL,
            SimklConnectionMode.AWAITING_APPROVAL.toTrackingConnectionCardMode(),
        )
        assertEquals(
            TrackingConnectionCardMode.CONNECTED,
            SimklConnectionMode.CONNECTED.toTrackingConnectionCardMode(),
        )
    }

    @Test
    fun `remote brands are available only while their provider is connected`() {
        assertTrue(isTrackingBrandAvailable(TrackingBrand.NUVIO, false))
        assertTrue(isTrackingBrandAvailable(TrackingBrand.TMDB, false))
        assertFalse(isTrackingBrandAvailable(TrackingBrand.SIMKL, simklConnected = false))
        assertTrue(isTrackingBrandAvailable(TrackingBrand.SIMKL, simklConnected = true))
        assertFalse(isTrackingBrandAvailable(TrackingBrand.MDBLIST, simklConnected = true, mdblistConnected = false))
        assertTrue(isTrackingBrandAvailable(TrackingBrand.MDBLIST, simklConnected = false, mdblistConnected = true))
    }

    @Test
    fun `stored recommendations preference survives without a connected provider`() {
        val stored = MoreLikeThisSourcePreference.TMDB

        assertEquals(MoreLikeThisSourcePreference.TMDB, effectiveTrackingRecommendationsSource(stored))
        assertEquals(MoreLikeThisSourcePreference.TMDB, stored)
        // Values persisted by the retired Trakt integration decode to the default source.
        assertEquals(MoreLikeThisSourcePreference.TMDB, MoreLikeThisSourcePreference.fromStorage("TRAKT"))
    }
}
