package com.nuvio.app.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class PosterEmphasisScaleTest {
    @Test
    fun televisionFocusHasStrongerLiftThanDesktopHover() {
        assertEquals(
            1.07f,
            interactionEmphasisScale(
                isDesktop = false,
                isTelevision = true,
                emphasized = true,
                motionPolicy = UiMotionPolicy(),
            ),
        )
        assertEquals(
            1.045f,
            interactionEmphasisScale(
                isDesktop = true,
                isTelevision = false,
                emphasized = true,
                motionPolicy = UiMotionPolicy(),
            ),
        )
    }

    @Test
    fun mobileAndReducedMotionDoNotScalePosterCards() {
        assertEquals(
            1f,
            interactionEmphasisScale(
                isDesktop = false,
                isTelevision = false,
                emphasized = true,
                motionPolicy = UiMotionPolicy(),
            ),
        )
        assertEquals(
            1f,
            interactionEmphasisScale(
                isDesktop = false,
                isTelevision = true,
                emphasized = true,
                motionPolicy = UiMotionPolicy(mode = NavigationMotion.REDUCED),
            ),
        )
        assertEquals(
            1f,
            interactionEmphasisScale(
                isDesktop = false,
                isTelevision = true,
                emphasized = false,
                motionPolicy = UiMotionPolicy(),
            ),
        )
    }
}
