package com.nuvio.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.LocalUiMotion
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.features.settings.AppBrandWordmark
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.app_brand_name
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.sin

internal const val OpeningBeamWidthFraction = 0.7f
private const val OpeningDurationMillis = 2_450

/** Shared cinematic opening used while the real app startup/profile work is in progress. */
@Composable
internal fun TelumiaOpeningMotion(modifier: Modifier = Modifier) {
    val motion = LocalUiMotion.current
    val palette = MaterialTheme.themePalette
    val appBrandName = stringResource(Res.string.app_brand_name)
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val progress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(
            durationMillis = motion.durationMillis(OpeningDurationMillis),
            easing = FastOutSlowInEasing,
        ),
        label = "telumiaOpeningSequence",
    )
    val openingProgress = if (motion.allowsSpatialEffects) progress else 1f
    val lockup = openingLockupMotionFrame(openingProgress)

    BoxWithConstraints(
        modifier = modifier
            .clipToBounds(),
    ) {
        OpeningAtmosphere(
            primary = palette.secondary,
            secondary = palette.secondaryVariant,
            modifier = Modifier.fillMaxSize(),
        )
        OpeningBeam(
            phase = openingProgress,
            primary = palette.secondary,
            secondary = palette.secondaryVariant,
            modifier = Modifier.fillMaxSize(),
        )

        val logoHeight = minOf((maxHeight * 0.15f).coerceIn(64.dp, 126.dp), maxWidth * 0.17f)
        val logoWidth = logoHeight * 4.1f
        val fontScale = LocalDensity.current.fontScale
        val sweepPosition = if (motion.allowsSpatialEffects) openingProgress else 0.5f
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .width(logoWidth)
                    .height(logoHeight)
                    .clearAndSetSemantics {
                        contentDescription = appBrandName
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppBrandWordmark(
                    modifier = Modifier
                        .height(logoHeight * 0.9f)
                        .width(logoHeight * 0.9f)
                        .graphicsLayer {
                            alpha = lockup.iconReveal
                            scaleX = lockup.iconScale
                            scaleY = lockup.iconScale
                            rotationZ = lockup.iconRotationDegrees
                            translationY = lockup.iconTranslationY * logoHeight.toPx()
                        },
                    compact = true,
                )
                Spacer(Modifier.width(logoHeight * 0.18f))
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clipToBounds()
                        .drawWithContent {
                            clipRect(right = size.width * lockup.wordmarkReveal) {
                                this@drawWithContent.drawContent()
                            }
                        },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = "TELUMIA",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = (logoHeight.value * 0.50f / fontScale).sp,
                            lineHeight = (logoHeight.value * 0.60f / fontScale).sp,
                            letterSpacing = (logoHeight.value * 0.012f / fontScale).sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.nuvio.colors.textPrimary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.graphicsLayer {
                            alpha = lockup.wordmarkReveal
                            translationX = (lockup.wordmarkReveal - 1f) * -18.dp.toPx()
                        },
                    )
                }
            }
            Spacer(Modifier.height(34.dp))
            OpeningLoadingSweep(
                progress = sweepPosition,
                accent = palette.secondary,
                modifier = Modifier.width(168.dp).height(3.dp),
            )
        }
    }
}

@Composable
private fun OpeningAtmosphere(
    primary: Color,
    secondary: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.drawWithCache {
        val maxDimension = maxOf(size.width, size.height)
        val primaryGlow = Brush.radialGradient(
            colors = listOf(primary.copy(alpha = 0.32f), Color.Transparent),
            center = Offset(size.width * 0.23f, size.height * 0.47f),
            radius = maxDimension * 0.78f,
        )
        val secondaryGlow = Brush.radialGradient(
            colors = listOf(secondary.copy(alpha = 0.18f), Color.Transparent),
            center = Offset(size.width * 0.82f, size.height * 0.52f),
            radius = maxDimension * 0.68f,
        )
        val vignette = Brush.radialGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                0.58f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.68f),
            ),
            center = Offset(size.width * 0.5f, size.height * 0.48f),
            radius = maxDimension * 0.78f,
        )
        onDrawBehind {
            drawRect(Color(0xFF06080D))
            drawRect(primaryGlow)
            drawRect(secondaryGlow)
            drawRect(vignette)
        }
    })
}

@Composable
private fun OpeningBeam(
    phase: Float,
    primary: Color,
    secondary: Color,
    modifier: Modifier = Modifier,
) {
    val beamPhase = ((phase - 0.12f) / 0.68f).coerceIn(0f, 1f)
    val beam = openingBeamMotionFrame(beamPhase)
    Canvas(modifier = modifier) {
        if (beam.alpha <= 0f) return@Canvas
        val startX = size.width * beam.startXFraction
        val endX = startX + size.width * OpeningBeamWidthFraction
        val beamBrush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                primary.copy(alpha = 0.52f),
                secondary.copy(alpha = 0.46f),
                Color.Transparent,
            ),
            startX = startX,
            endX = endX,
        )
        drawLine(
            brush = beamBrush,
            start = Offset(startX, size.height * 0.69f),
            end = Offset(endX, size.height * 0.47f),
            strokeWidth = maxOf(size.width, size.height) * 0.032f,
            cap = StrokeCap.Round,
            alpha = beam.alpha * 0.66f,
        )
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.66f), Color.Transparent),
                startX = startX,
                endX = endX,
            ),
            start = Offset(startX, size.height * 0.69f),
            end = Offset(endX, size.height * 0.47f),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
            alpha = beam.alpha * 0.54f,
        )
    }
}

internal data class OpeningLockupMotionFrame(
    val iconReveal: Float,
    val iconScale: Float,
    val iconRotationDegrees: Float,
    val iconTranslationY: Float,
    val wordmarkReveal: Float,
)

internal fun openingLockupMotionFrame(progress: Float): OpeningLockupMotionFrame {
    val phase = progress.coerceIn(0f, 1f)
    val icon = smoothOpeningStep(0.10f, 0.52f, phase)
    val wordmark = smoothOpeningStep(0.34f, 0.76f, phase)
    val overshoot = 0.46f * sin(PI * icon).toFloat() * (1f - icon)
    return OpeningLockupMotionFrame(
        iconReveal = icon,
        iconScale = 0.68f + icon * 0.32f + overshoot,
        iconRotationDegrees = (1f - icon) * -15f,
        iconTranslationY = (1f - icon) * 0.24f,
        wordmarkReveal = wordmark,
    )
}

private fun smoothOpeningStep(start: Float, end: Float, progress: Float): Float {
    val value = ((progress - start) / (end - start)).coerceIn(0f, 1f)
    return value * value * (3f - 2f * value)
}

internal data class OpeningBeamMotionFrame(
    val startXFraction: Float,
    val alpha: Float,
)

internal fun openingBeamMotionFrame(progress: Float): OpeningBeamMotionFrame {
    val phase = progress.coerceIn(0f, 1f)
    return OpeningBeamMotionFrame(
        startXFraction = -0.35f + (phase - 0.5f) * 1.6f,
        alpha = sin(PI * phase).toFloat().coerceIn(0f, 1f),
    )
}

@Composable
private fun OpeningLoadingSweep(
    progress: Float,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(
            color = accent.copy(alpha = 0.22f),
            cornerRadius = radius,
        )

        val segmentWidth = size.width * 0.30f
        val left = (size.width + segmentWidth) * progress.coerceIn(0f, 1f) - segmentWidth
        drawRoundRect(
            color = accent.copy(alpha = 0.24f),
            topLeft = Offset(left - segmentWidth * 0.18f, -size.height * 0.45f),
            size = Size(segmentWidth * 1.36f, size.height * 1.9f),
            cornerRadius = CornerRadius(size.height),
        )
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    accent.copy(alpha = 0.68f),
                    Color.White.copy(alpha = 0.92f),
                    accent.copy(alpha = 0.68f),
                    Color.Transparent,
                ),
                startX = left,
                endX = left + segmentWidth,
            ),
            topLeft = Offset(left, 0f),
            size = Size(segmentWidth, size.height),
            cornerRadius = radius,
        )
    }
}
