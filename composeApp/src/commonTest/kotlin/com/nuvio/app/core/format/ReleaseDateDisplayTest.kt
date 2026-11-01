package com.nuvio.app.core.format

import kotlin.test.Test
import kotlin.test.assertEquals

class ReleaseDateDisplayTest {
    @Test
    fun partialYearDoesNotShowTrailingSeparator() {
        assertEquals("2026", formatReleaseDateForDisplay("2026-"))
        assertEquals("2026", formatReleaseDateWithoutYear("2026-"))
    }

    @Test
    fun completeYearRemainsUnchanged() {
        assertEquals("2026", formatReleaseDateForDisplay("2026"))
        assertEquals("2026", formatReleaseDateWithoutYear("2026"))
    }
}
