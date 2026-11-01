package com.nuvio.app

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveTvNavigationAvailabilityTest {
    @Test
    fun liveTvDestinationIsVisibleOnDesktopAndAndroidTv() {
        assertTrue(shouldExposeLiveTvDestination(isDesktop = true, isTelevision = false))
        assertTrue(shouldExposeLiveTvDestination(isDesktop = false, isTelevision = true))
    }

    @Test
    fun liveTvDestinationRemainsHiddenOnTouchOnlyMobile() {
        assertFalse(shouldExposeLiveTvDestination(isDesktop = false, isTelevision = false))
    }
}
