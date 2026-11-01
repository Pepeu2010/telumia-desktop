package com.nuvio.app.core.ui

private const val DesktopHoverEmphasisScale = 1.045f
private const val TelevisionFocusEmphasisScale = 1.07f

internal fun interactionEmphasisScale(
    isDesktop: Boolean,
    isTelevision: Boolean,
    emphasized: Boolean,
    motionPolicy: UiMotionPolicy,
): Float {
    if (!emphasized || !motionPolicy.allowsSpatialEffects) return 1f
    val target = when {
        isTelevision -> TelevisionFocusEmphasisScale
        isDesktop -> DesktopHoverEmphasisScale
        else -> 1f
    }
    return motionPolicy.scale(target)
}
