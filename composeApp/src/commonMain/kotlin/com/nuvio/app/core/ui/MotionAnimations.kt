package com.nuvio.app.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember

@Composable
internal fun rememberUiMotionLoopFloat(
    active: Boolean,
    initialValue: Float,
    targetValue: Float,
    durationMillis: Int,
    label: String,
    repeatMode: RepeatMode = RepeatMode.Restart,
): State<Float> {
    val motion = LocalUiMotion.current
    return if (active && motion.allowsSpatialEffects) {
        rememberInfiniteTransition(label = label).animateFloat(
            initialValue = initialValue,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = motion.durationMillis(durationMillis), easing = LinearEasing),
                repeatMode = repeatMode,
            ),
            label = "${label}Value",
        )
    } else {
        remember(initialValue) { mutableFloatStateOf(initialValue) }
    }
}
