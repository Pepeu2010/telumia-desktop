package com.nuvio.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.LocalScreenActive
import com.nuvio.app.core.ui.LocalUiMotion
import com.nuvio.app.core.ui.rememberUiMotionLoopFloat
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.features.settings.AppBrandWordmark
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.app_brand_name
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.sin

internal const val OpeningBeamWidthFraction = 0.7f
private const val OpeningDurationMillis = 2_200

/** Shared cinematic opening used while the real app startup/profile work is in progress. */
@Composable
internal fun TelumiaOpeningMotion(modifier: Modifier = Modifier) {
    val motion = LocalUiMotion.current
    val active = LocalScreenActive.current
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
    val lockup = openingLockupMotionFrame(progress)
    val loadingSweep = rememberUiMotionLoopFloat(
        active = active,
        initialValue = 0f,
        targetValue = 1f,
        durationMillis = 1_350,
        label = "telumiaOpeningLoadingSweep",
    )

    BoxWithConstraints(
        modifier = modifier
            .clipToBounds(),
    ) {
        OpeningAtmosphere(
            phase = progress,
            primary = palette.secondary,
            secondary = palette.secondaryVariant,
            modifier = Modifier.fillMaxSize(),
        )

        val logoHeight = minOf((maxHeight * 0.15f).coerceIn(64.dp, 126.dp), maxWidth * 0.17f)
        val logoWidth = logoHeight * 4.1f
        val fontScale = LocalDensity.current.fontScale
        val sweepPosition = if (active && motion.allowsSpatialEffects) loadingSweep.value else 0.5f
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
                modifier = Modifier.width(112.dp).height(2.dp),
            )
        }
    }
}

@Composable
private fun OpeningAtmosphere(
    phase: Float,
    primary: Color,
    secondary: Color,
    modifier: Modifier = Modifier,
) {
    val beamPhase = ((phase - 0.12f) / 0.68f).coerceIn(0f, 1f)
    val beam = openingBeamMotionFrame(beamPhase)
    Canvas(modifier = modifier) {
        val maxDimension = maxOf(size.width, size.height)
        val minDimension = minOf(size.width, size.height)
        val beamStartX = size.width * beam.startXFraction
        val beamEndX = beamStartX + size.width * OpeningBeamWidthFraction

        drawRect(color = Color.Black.copy(alpha = 0.32f), size = size)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.24f), Color.Transparent),
                center = Offset(size.width * (0.35f + phase * 0.30f), size.height * 0.48f),
                radius = maxDimension * 0.48f,
            ),
            size = size,
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(size.width * 0.82f, size.height * 0.56f),
                radius = maxDimension * 0.38f,
            ),
            size = size,
        )

        val center = Offset(size.width * 0.5f, size.height * 0.48f)
        for (ring in 0..2) {
            val start = 0.22f + ring * 0.11f
            val ringProgress = ((phase - start) / 0.58f).coerceIn(0f, 1f)
            if (phase in start..(start + 0.58f)) {
                drawCircle(
                    color = if (ring == 1) secondary else primary,
                    radius = minDimension * (0.08f + ringProgress * 0.46f),
                    center = center,
                    alpha = (1f - ringProgress) * 0.16f,
                    style = Stroke(width = (1.0f + ring * 0.35f).dp.toPx()),
                )
            }
        }

        for (particle in 0 until 22) {
            val xSeed = (particle * 37 % 101) / 100f
            val ySeed = (particle * 61 % 97) / 96f
            val twinkle = ((sin(phase * PI * 2.0 + particle * 1.71) + 1.0) * 0.5).toFloat()
            val particleAlpha = (0.025f + twinkle * 0.075f) * (1f - phase * 0.45f)
            drawCircle(
                color = if (particle % 4 == 0) secondary else Color.White,
                radius = (0.65f + twinkle * 0.75f).dp.toPx(),
                center = Offset(size.width * xSeed, size.height * ySeed),
                alpha = particleAlpha,
            )
        }

        val lightRibbon = Path().apply {
            moveTo(beamStartX, size.height * 0.70f)
            cubicTo(
                beamStartX + size.width * 0.18f,
                size.height * 0.43f,
                beamEndX - size.width * 0.14f,
                size.height * 0.76f,
                beamEndX,
                size.height * 0.47f,
            )
        }
        val ribbonBrush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                primary.copy(alpha = 0.48f),
                secondary.copy(alpha = 0.40f),
                Color.Transparent,
            ),
            startX = beamStartX,
            endX = beamEndX,
        )
        drawPath(
            path = lightRibbon,
            brush = ribbonBrush,
            alpha = beam.alpha * 0.48f,
            style = Stroke(width = maxDimension * 0.035f, cap = StrokeCap.Round),
        )
        drawPath(
            path = lightRibbon,
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.46f), Color.Transparent),
                startX = beamStartX,
                endX = beamEndX,
            ),
            alpha = beam.alpha * 0.58f,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
        )
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    0.56f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.72f),
                ),
                center = Offset(size.width * 0.5f, size.height * 0.48f),
                radius = maxDimension * 0.76f,
            ),
            size = size,
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
    val overshoot = 0.32f * sin(PI * icon).toFloat() * (1f - icon)
    return OpeningLockupMotionFrame(
        iconReveal = icon,
        iconScale = 0.76f + icon * 0.24f + overshoot,
        iconRotationDegrees = (1f - icon) * -8f,
        iconTranslationY = (1f - icon) * 0.14f,
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
