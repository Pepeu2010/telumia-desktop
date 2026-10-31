package com.nuvio.app

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class AppOpeningMotionTest {
    @Test
    fun iconAndWordmarkRevealInSequenceAndTheIconSettlesWithOvershoot() {
        val start = openingLockupMotionFrame(0f)
        val iconArrival = openingLockupMotionFrame(0.31f)
        val final = openingLockupMotionFrame(1f)

        assertTrue(start.iconReveal == 0f, "the mark should begin hidden")
        assertTrue(start.wordmarkReveal == 0f, "the wordmark should follow the mark")
        assertTrue(iconArrival.iconReveal > iconArrival.wordmarkReveal, "the mark should lead the wordmark reveal")
        assertTrue(iconArrival.iconScale > 1f, "the mark should overshoot before settling")
        assertTrue(final.iconReveal == 1f && final.wordmarkReveal == 1f, "the full lockup should be revealed")
        assertTrue(abs(final.iconScale - 1f) < AlphaTolerance, "the mark should settle at its original scale")
        assertTrue(abs(final.iconRotationDegrees) < AlphaTolerance, "the mark should settle without rotation")
        assertTrue(abs(final.iconTranslationY) < AlphaTolerance, "the mark should settle at its original position")
    }

    @Test
    fun cinematicLockupClampsProgressToItsFiniteSequence() {
        val before = openingLockupMotionFrame(-1f)
        val start = openingLockupMotionFrame(0f)
        val after = openingLockupMotionFrame(2f)
        val end = openingLockupMotionFrame(1f)

        assertTrue(before == start, "negative progress should hold the first frame")
        assertTrue(after == end, "progress above one should hold the settled frame")
    }

    @Test
    fun cinematicBeamCrossesTheWordmarkAndFadesAtTheEdges() {
        val start = openingBeamMotionFrame(0f)
        val midpoint = openingBeamMotionFrame(0.5f)
        val end = openingBeamMotionFrame(1f)

        assertTrue(start.startXFraction + OpeningBeamWidthFraction < 0f, "beam should begin outside the left edge")
        assertTrue(midpoint.startXFraction < 0f, "beam should cross the center from the left")
        assertTrue(midpoint.startXFraction + OpeningBeamWidthFraction > 0f, "beam should cross the center to the right")
        assertTrue(end.startXFraction > 0f, "beam should finish outside the right edge")
        assertTrue(abs(start.alpha) < AlphaTolerance, "beam should fade in from black")
        assertTrue(midpoint.alpha > 0.99f, "beam should peak as it crosses the logo")
        assertTrue(abs(end.alpha) < AlphaTolerance, "beam should fade out after crossing")
    }

    @Test
    fun cinematicBeamProgressIsClampedToItsOneShotRange() {
        assertTrue(openingBeamMotionFrame(-1f).alpha in 0f..1f)
        assertTrue(openingBeamMotionFrame(2f).alpha in 0f..1f)
        assertTrue(openingBeamMotionFrame(-1f).startXFraction == openingBeamMotionFrame(0f).startXFraction)
        assertTrue(openingBeamMotionFrame(2f).startXFraction == openingBeamMotionFrame(1f).startXFraction)
    }

    private companion object {
        const val AlphaTolerance = 0.0001f
    }
}
