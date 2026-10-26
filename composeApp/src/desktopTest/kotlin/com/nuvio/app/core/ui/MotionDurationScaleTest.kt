package com.nuvio.app.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class MotionDurationScaleTest {
    @Test
    fun reducedMotionShortensUnmappedComposeAnimationsAndPreservesSystemScale() {
        val scale = UiAnimationDurationScale()
        assertEquals(1f, scale.scaleFactor)

        scale.mode = NavigationMotion.REDUCED
        assertEquals(0.5f, scale.scaleFactor)

        scale.systemScale = 0.8f
        assertEquals(0.4f, scale.scaleFactor)

        scale.mode = NavigationMotion.OFF
        assertEquals(0f, scale.scaleFactor)
    }
}
