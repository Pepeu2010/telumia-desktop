package com.nuvio.app.features.home.components

import kotlin.test.Test
import kotlin.test.assertEquals

class HomePosterPreviewPolicyTest {
    @Test
    fun previewCardScalesForTelevisionReadability() {
        assertEquals(520, homePosterPreviewWidthDp(isDesktop = false, isTelevision = true))
    }

    @Test
    fun desktopPreviewWidthStaysCompactAndTouchPlatformsKeepDefaultWidth() {
        assertEquals(448, homePosterPreviewWidthDp(isDesktop = true, isTelevision = false))
        assertEquals(420, homePosterPreviewWidthDp(isDesktop = false, isTelevision = false))
    }

    @Test
    fun desktopSupportsHoverAndFocusPreviewsWithTrailers() {
        assertEquals(
            HomePosterPreviewAvailability(interactivePreview = true, trailerPlayback = true),
            resolveHomePosterPreviewAvailability(
                isDesktop = true,
                isTelevision = false,
                platformSupportsTrailerPlayback = true,
            ),
        )
    }

    @Test
    fun androidTvSupportsDpadPreviewAndTrailerPlayback() {
        assertEquals(
            HomePosterPreviewAvailability(interactivePreview = true, trailerPlayback = true),
            resolveHomePosterPreviewAvailability(
                isDesktop = false,
                isTelevision = true,
                platformSupportsTrailerPlayback = true,
            ),
        )
    }

    @Test
    fun touchOnlyAndroidDoesNotOpenHoverPreviewCards() {
        assertEquals(
            HomePosterPreviewAvailability(interactivePreview = false, trailerPlayback = false),
            resolveHomePosterPreviewAvailability(
                isDesktop = false,
                isTelevision = false,
                platformSupportsTrailerPlayback = true,
            ),
        )
    }

    @Test
    fun previewNeverStartsTrailerWhenPlatformCannotPlayIt() {
        assertEquals(
            HomePosterPreviewAvailability(interactivePreview = true, trailerPlayback = false),
            resolveHomePosterPreviewAvailability(
                isDesktop = false,
                isTelevision = true,
                platformSupportsTrailerPlayback = false,
            ),
        )
    }

    @Test
    fun heroTrailerRequiresStableInteractionAndEnabledPreferences() {
        val desktop = resolveHomePosterPreviewAvailability(
            isDesktop = true,
            isTelevision = false,
            platformSupportsTrailerPlayback = true,
        )

        assertEquals(
            true,
            shouldStartHomeHeroTrailerPreview(
                availability = desktop,
                posterPreviewEnabled = true,
                trailerPreferenceEnabled = true,
                screenActive = true,
                pointerOrFocusActive = true,
                pageSettled = true,
                zoomOverlayVisible = false,
            ),
        )
        assertEquals(
            false,
            shouldStartHomeHeroTrailerPreview(
                availability = desktop,
                posterPreviewEnabled = true,
                trailerPreferenceEnabled = true,
                screenActive = true,
                pointerOrFocusActive = true,
                pageSettled = false,
                zoomOverlayVisible = false,
            ),
        )
        assertEquals(
            false,
            shouldStartHomeHeroTrailerPreview(
                availability = desktop,
                posterPreviewEnabled = true,
                trailerPreferenceEnabled = true,
                screenActive = false,
                pointerOrFocusActive = true,
                pageSettled = true,
                zoomOverlayVisible = false,
            ),
        )
    }

    @Test
    fun heroTrailerDoesNotAutoplayOnTouchOnlyDevicesOrWhenDisabled() {
        val phone = resolveHomePosterPreviewAvailability(
            isDesktop = false,
            isTelevision = false,
            platformSupportsTrailerPlayback = true,
        )
        assertEquals(
            false,
            shouldStartHomeHeroTrailerPreview(
                availability = phone,
                posterPreviewEnabled = true,
                trailerPreferenceEnabled = true,
                screenActive = true,
                pointerOrFocusActive = true,
                pageSettled = true,
                zoomOverlayVisible = false,
            ),
        )

        val tv = resolveHomePosterPreviewAvailability(
            isDesktop = false,
            isTelevision = true,
            platformSupportsTrailerPlayback = true,
        )
        assertEquals(
            false,
            shouldStartHomeHeroTrailerPreview(
                availability = tv,
                posterPreviewEnabled = true,
                trailerPreferenceEnabled = false,
                screenActive = true,
                pointerOrFocusActive = true,
                pageSettled = true,
                zoomOverlayVisible = false,
            ),
        )
    }
}
