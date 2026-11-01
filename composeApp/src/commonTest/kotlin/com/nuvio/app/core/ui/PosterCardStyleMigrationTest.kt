package com.nuvio.app.core.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PosterCardStyleMigrationTest {
    @Test
    fun enablesTvTrailerPreviewForLegacyAndroidPreferences() {
        assertTrue(
            migrateHoverPreviewTrailerPreference(
                storedEnabled = false,
                schemaVersion = 0,
                platformSupportsTrailerPlayback = true,
                isDesktop = false,
            ),
        )
    }

    @Test
    fun preservesDesktopOptOutDuringMigration() {
        assertFalse(
            migrateHoverPreviewTrailerPreference(
                storedEnabled = false,
                schemaVersion = 0,
                platformSupportsTrailerPlayback = true,
                isDesktop = true,
            ),
        )
    }

    @Test
    fun preservesAnExplicitOptOutAfterMigration() {
        assertFalse(
            migrateHoverPreviewTrailerPreference(
                storedEnabled = false,
                schemaVersion = 1,
                platformSupportsTrailerPlayback = true,
                isDesktop = false,
            ),
        )
    }

    @Test
    fun doesNotEnableTrailersWhereThePlatformCannotPlayThem() {
        assertFalse(
            migrateHoverPreviewTrailerPreference(
                storedEnabled = false,
                schemaVersion = 0,
                platformSupportsTrailerPlayback = false,
                isDesktop = false,
            ),
        )
    }
}
