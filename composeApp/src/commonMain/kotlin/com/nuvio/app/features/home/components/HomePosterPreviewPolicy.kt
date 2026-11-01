package com.nuvio.app.features.home.components

internal data class HomePosterPreviewAvailability(
    val interactivePreview: Boolean,
    val trailerPlayback: Boolean,
)

internal fun resolveHomePosterPreviewAvailability(
    isDesktop: Boolean,
    isTelevision: Boolean,
    platformSupportsTrailerPlayback: Boolean,
): HomePosterPreviewAvailability {
    val interactivePreview = isDesktop || isTelevision
    return HomePosterPreviewAvailability(
        interactivePreview = interactivePreview,
        trailerPlayback = interactivePreview && platformSupportsTrailerPlayback,
    )
}

internal fun homePosterPreviewWidthDp(
    isDesktop: Boolean,
    isTelevision: Boolean,
): Int = when {
    isTelevision -> 520
    isDesktop -> 448
    else -> 420
}

internal fun shouldStartHomeHeroTrailerPreview(
    availability: HomePosterPreviewAvailability,
    posterPreviewEnabled: Boolean,
    trailerPreferenceEnabled: Boolean,
    screenActive: Boolean,
    pointerOrFocusActive: Boolean,
    pageSettled: Boolean,
    zoomOverlayVisible: Boolean,
): Boolean = availability.trailerPlayback &&
    posterPreviewEnabled &&
    trailerPreferenceEnabled &&
    screenActive &&
    pointerOrFocusActive &&
    pageSettled &&
    !zoomOverlayVisible
