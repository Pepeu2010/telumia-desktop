package com.nuvio.app.features.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.nuvio.app.isDesktop
import com.nuvio.app.isWindows
import com.nuvio.app.isTelevisionDevice
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.ui.FullscreenActionButton
import com.nuvio.app.core.ui.DesktopBackdropVerticalBias
import com.nuvio.app.core.ui.NuvioDesktopImageScaling
import com.nuvio.app.core.ui.NuvioAsyncImage as AsyncImage
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.LocalScreenActive
import com.nuvio.app.core.ui.PosterZoomOverlayCoordinator
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.core.ui.isFullscreenActionSupported
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.ui.heroStretchHeight
import com.nuvio.app.core.ui.ScreenActivityEffect
import com.nuvio.app.core.ui.heroStretchZoom
import com.nuvio.app.core.ui.ultrawideViewportProgress
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.tmdb.originalTmdbImageUrl
import com.nuvio.app.features.trailer.TrailerPlaybackSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

private const val HERO_BACKGROUND_PARALLAX = 0.055f
private const val HERO_BACKGROUND_SCALE = 1.14f
private const val HERO_CONTENT_PARALLAX = 0.18f
private const val HERO_SCROLL_PARALLAX = 0.3f
private const val DESKTOP_HERO_SCROLL_PARALLAX = 0.38f
private const val HERO_SCROLL_DOWN_SCALE_MULTIPLIER = 0.0001f
private const val HERO_SCROLL_UP_SCALE_MULTIPLIER = 0.002f
private const val HERO_SCROLL_MAX_SCALE = 1.3f
private const val HERO_SWIPE_THRESHOLD_FRACTION = 0.16f
private const val HERO_SWIPE_VELOCITY_THRESHOLD = 300f
private const val HERO_AUTO_SCROLL_INTERVAL_MS = 8_000L
private const val MOBILE_HERO_VIEWPORT_RATIO = 0.82f
private const val MOBILE_HERO_MIN_HEIGHT_DP = 360f
private const val MOBILE_HERO_MAX_HEIGHT_DP = 760f
private const val ULTRAWIDE_HERO_VIEWPORT_HEIGHT_RATIO = 1f
private const val CINEMATIC_DESKTOP_HERO_VIEWPORT_RATIO = 0.68f
private const val CINEMATIC_TV_MIN_WIDTH_DP = 1000f
private const val DESKTOP_HERO_ULTRAWIDE_HORIZONTAL_PADDING_DP = 120f
private const val DESKTOP_HERO_ULTRAWIDE_BOTTOM_PADDING_DP = 192f
private const val DESKTOP_HERO_TOP_FADE_HEIGHT_DP = 160f
private const val DESKTOP_HERO_BOTTOM_FADE_HEIGHT_DP = 300f

internal data class HomeHeroLayout(
    val isTablet: Boolean,
    val heroHeight: Dp,
    val contentMaxWidth: Dp,
    val contentContainerMaxWidth: Dp,
    val contentWidthFraction: Float,
    val contentHorizontalPadding: Dp,
    val contentVerticalPadding: Dp,
    val topFadeHeight: Dp,
    val bottomFadeHeight: Dp,
    val logoWidthFraction: Float,
    val backgroundMotionStrength: Float,
)

@Composable
fun HomeHeroSection(
    items: List<MetaPreview>,
    modifier: Modifier = Modifier,
    viewportHeight: Dp? = null,
    mobileBelowSectionHeightHint: Dp? = null,
    sectionPadding: Dp? = null,
    listState: LazyListState? = null,
    stretchPx: () -> Float = { 0f },
    onItemClick: ((MetaPreview) -> Unit)? = null,
    previewTrailersSupported: Boolean = AppFeaturePolicy.hoverTrailerPlaybackSupported,
) {
    if (items.isEmpty()) return

    val pagerState = key(items.size) {
        rememberPagerState(
            initialPage = if (items.size > 1) {
                val middle = Int.MAX_VALUE / 2
                middle - middle % items.size
            } else {
                0
            },
            pageCount = { if (items.size > 1) Int.MAX_VALUE else items.size },
        )
    }
    val coroutineScope = rememberCoroutineScope()
    var pagerDragActive by remember { mutableStateOf(false) }
    val heroInteractionSource = remember { MutableInteractionSource() }
    val heroHovered by heroInteractionSource.collectIsHoveredAsState()
    var heroFocused by remember { mutableStateOf(false) }
    val autoScrollPage = pagerState.settledPage
    val posterCardStyle = rememberPosterCardStyleUiState()
    val previewAvailability = resolveHomePosterPreviewAvailability(
        isDesktop = isDesktop,
        isTelevision = isTelevisionDevice(),
        platformSupportsTrailerPlayback = previewTrailersSupported &&
            AppFeaturePolicy.hoverTrailerPlaybackSupported,
    )
    val isScreenActive = LocalScreenActive.current
    val activeHeroItem = items[pagerState.settledPage % items.size]
    val heroTrailerOwner = remember { Any() }
    val activePreviewOwner by homePosterPreviewOwnership.active.collectAsState()
    val ownsHeroTrailer = activePreviewOwner === heroTrailerOwner
    var heroTrailerPlaybackSource by remember { mutableStateOf<TrailerPlaybackSource?>(null) }

    DisposableEffect(heroTrailerOwner) {
        onDispose { homePosterPreviewOwnership.release(heroTrailerOwner) }
    }

    LaunchedEffect(
        activeHeroItem.type,
        activeHeroItem.id,
        heroHovered,
        heroFocused,
        pagerDragActive,
        pagerState.isScrollInProgress,
        posterCardStyle.hoverPreviewEnabled,
        posterCardStyle.hoverPreviewTrailerEnabled,
        posterCardStyle.hoverPreviewOpenDelayMillis,
        isScreenActive,
        PosterZoomOverlayCoordinator.isVisible,
    ) {
        heroTrailerPlaybackSource = null
        val shouldStartPreview = shouldStartHomeHeroTrailerPreview(
            availability = previewAvailability,
            posterPreviewEnabled = posterCardStyle.hoverPreviewEnabled,
            trailerPreferenceEnabled = posterCardStyle.hoverPreviewTrailerEnabled,
            screenActive = isScreenActive,
            pointerOrFocusActive = heroHovered || heroFocused,
            pageSettled = !pagerDragActive && !pagerState.isScrollInProgress,
            zoomOverlayVisible = PosterZoomOverlayCoordinator.isVisible,
        )
        if (!shouldStartPreview) {
            homePosterPreviewOwnership.release(heroTrailerOwner)
            return@LaunchedEffect
        }

        delay(posterCardStyle.hoverPreviewOpenDelayMillis.toLong())
        homePosterPreviewOwnership.claim(heroTrailerOwner)
        val source = resolveBoundedHoverPreview {
            resolveHomePosterHoverTrailerPlaybackSource(activeHeroItem)
        }
        if (homePosterPreviewOwnership.active.value === heroTrailerOwner) {
            heroTrailerPlaybackSource = source
        }
    }

    LaunchedEffect(ownsHeroTrailer) {
        if (!ownsHeroTrailer) heroTrailerPlaybackSource = null
    }

    LaunchedEffect(pagerState) {
        pagerState.scrollToPage(pagerState.currentPage)
    }

    ScreenActivityEffect(pagerState) { active ->
        if (!active) {
            pagerState.stopScroll(MutatePriority.PreventUserInput)
            pagerState.scrollToPage(pagerState.currentPage)
        }
    }

    ScreenActivityEffect(autoScrollPage, items.size, heroHovered, heroFocused, pagerDragActive) { active ->
        if (!active || items.size <= 1 || heroHovered || heroFocused || pagerDragActive) return@ScreenActivityEffect
        delay(HERO_AUTO_SCROLL_INTERVAL_MS)
        while (pagerState.isScrollInProgress) {
            delay(100L)
        }

        val nextPage = pagerState.currentPage + 1
        pagerState.animateScrollToPage(nextPage)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(heroInteractionSource)
            .onFocusChanged { heroFocused = it.hasFocus }
            .focusGroup()
            .homeHeroPagerGesture(
                pagerState = pagerState,
                itemCount = items.size,
                coroutineScope = coroutineScope,
                onDragActiveChange = { pagerDragActive = it },
            )
            .then(
                if (isDesktop) {
                    Modifier.graphicsLayer { clip = true }
                } else {
                    Modifier.clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                },
            ),
    ) {
        val useCinematicLayout = shouldUseCinematicHomeLayout(
            isDesktop = isDesktop,
            isTelevision = isTelevisionDevice(),
            maxWidthDp = maxWidth.value,
        )
        val layout = homeHeroLayout(
            maxWidthDp = maxWidth.value,
            viewportHeightDp = viewportHeight?.value,
            mobileBelowSectionHeightHintDp = mobileBelowSectionHeightHint?.value,
            preferDesktopLayout = useCinematicLayout,
        )
        val heroWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heroHeightPx = with(LocalDensity.current) { layout.heroHeight.toPx() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heroStretchHeight(layout.heroHeight, stretchPx),
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.01f },
            ) {
                Box(modifier = Modifier.fillMaxSize())
            }

            if (useCinematicLayout) {
                DesktopHomeHeroFrame(
                    items = items,
                    pagerState = pagerState,
                    listState = listState,
                    layout = layout,
                    heroWidthPx = heroWidthPx,
                    heroHeightPx = heroHeightPx,
                    stretchPx = stretchPx,
                    includePagerNeighbors = pagerDragActive,
                    contentHorizontalPadding = maxOf(
                        sectionPadding ?: layout.contentHorizontalPadding,
                        layout.contentHorizontalPadding,
                    ),
                    coroutineScope = coroutineScope,
                    onItemClick = onItemClick,
                    trailerPlaybackSource = heroTrailerPlaybackSource.takeIf { ownsHeroTrailer },
                    trailerSoundEnabled = posterCardStyle.hoverPreviewTrailerSoundEnabled,
                    trailerStartPositionSeconds = posterCardStyle.hoverPreviewTrailerStartSeconds,
                )
            } else {
                DefaultHomeHeroFrame(
                    items = items,
                    pagerState = pagerState,
                    listState = listState,
                    layout = layout,
                    heroWidthPx = heroWidthPx,
                    heroHeightPx = heroHeightPx,
                    stretchPx = stretchPx,
                    includePagerNeighbors = pagerDragActive,
                    coroutineScope = coroutineScope,
                    onItemClick = onItemClick,
                    trailerPlaybackSource = heroTrailerPlaybackSource.takeIf { ownsHeroTrailer },
                    trailerSoundEnabled = posterCardStyle.hoverPreviewTrailerSoundEnabled,
                    trailerStartPositionSeconds = posterCardStyle.hoverPreviewTrailerStartSeconds,
                )
            }
        }
    }
}

@Composable
private fun HeroBackgroundLayers(
    items: List<MetaPreview>,
    pagerState: PagerState,
    listState: LazyListState?,
    layout: HomeHeroLayout,
    heroWidthPx: Float,
    heroHeightPx: Float,
    stretchPx: () -> Float,
    includePagerNeighbors: Boolean,
    desktopFrame: Boolean = false,
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(layout.heroHeight)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF263944),
                        Color(0xFF192C33),
                        Color(0xFF10191E),
                        backgroundColor,
                    ),
                ),
            ),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(layout.heroHeight)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF45606A).copy(alpha = 0.32f), Color.Transparent),
                    radius = maxOf(heroWidthPx, heroHeightPx) * 0.78f,
                ),
            ),
    )

    val layerPages = rememberHeroLayerPages(
        pagerState = pagerState,
        itemCount = items.size,
        includePagerNeighbors = includePagerNeighbors,
    )

    val allowsSpatialEffects = com.nuvio.app.core.ui.LocalUiMotion.current.allowsSpatialEffects
    val backgroundMotionStrength = if (allowsSpatialEffects) { if (desktopFrame) layout.backgroundMotionStrength else 1f } else 0f
    layerPages.forEach { page ->
        val item = items[page % items.size]
        val imageUrl = item.banner ?: item.poster
        val backgroundModifier = if (desktopFrame) {
            Modifier
                .fillMaxSize()
                .heroStretchZoom { stretchPx() * backgroundMotionStrength }
        } else {
            Modifier
                .fillMaxWidth()
                .height(layout.heroHeight)
                .heroStretchZoom { if (allowsSpatialEffects) stretchPx() else 0f }
        }
        AsyncImage(
            model = if (desktopFrame) originalTmdbImageUrl(imageUrl) else imageUrl,
            contentDescription = item.name,
            modifier = backgroundModifier
                .graphicsLayer {
                    val pageOffset = heroPageOffset(pagerState, page)
                    val scrollOffsetPx = if (allowsSpatialEffects) heroScrollOffsetPx(listState, heroHeightPx) else 0f
                    val scrollScale = if (desktopFrame) {
                        1f + (heroBackgroundScrollScale(scrollOffsetPx) - 1f) * backgroundMotionStrength
                    } else {
                        heroBackgroundScrollScale(scrollOffsetPx)
                    }

                    alpha = heroPageVisibility(pageOffset)
                    translationX = if (allowsSpatialEffects) -pageOffset * heroWidthPx * HERO_BACKGROUND_PARALLAX else 0f
                    translationY = if (desktopFrame) {
                        heroDesktopBackgroundScrollTranslationY(scrollOffsetPx) * backgroundMotionStrength
                    } else {
                        heroBackgroundScrollTranslationY(scrollOffsetPx)
                    }
                    val baseScale = if (desktopFrame) 1f else HERO_BACKGROUND_SCALE
                    scaleX = baseScale * scrollScale
                    scaleY = baseScale * scrollScale
                },
            alignment = when {
                desktopFrame -> BiasAlignment(
                    horizontalBias = 0f,
                    verticalBias = DesktopBackdropVerticalBias,
                )
                layout.isTablet -> Alignment.TopCenter
                else -> Alignment.Center
            },
            contentScale = ContentScale.Crop,
            desktopImageScaling = NuvioDesktopImageScaling.Disabled,
        )
    }
}

@Composable
private fun HeroContentLayers(
    items: List<MetaPreview>,
    pagerState: PagerState,
    layout: HomeHeroLayout,
    heroWidthPx: Float,
    onItemClick: ((MetaPreview) -> Unit)?,
    includePagerNeighbors: Boolean,
) {
    val layerPages = rememberHeroLayerPages(
        pagerState = pagerState,
        itemCount = items.size,
        includePagerNeighbors = includePagerNeighbors,
    )

    val allowsSpatialEffects = com.nuvio.app.core.ui.LocalUiMotion.current.allowsSpatialEffects
    val contentLiftPx = with(LocalDensity.current) { 18.dp.toPx() }
    layerPages.forEach { page ->
        Box(
            modifier = Modifier.homeHeroContentTransition(
                pageOffset = heroPageOffset(pagerState, page),
                heroWidthPx = heroWidthPx,
                contentLiftPx = contentLiftPx,
                allowsSpatialEffects = allowsSpatialEffects,
            ),
        ) {
            HeroContentBlock(
                item = items[page % items.size],
                layout = layout,
                onItemClick = onItemClick,
            )
        }
    }
}

@Composable
private fun rememberHeroLayerPages(
    pagerState: PagerState,
    itemCount: Int,
    includePagerNeighbors: Boolean,
): List<Int> {
    if (itemCount <= 0) return emptyList()

    val currentPage = pagerState.currentPage
    val includeNeighbors = includePagerNeighbors || pagerState.isScrollInProgress
    return remember(currentPage, includeNeighbors, itemCount) {
        heroLayerPages(
            currentPage = currentPage,
            pageCount = pagerState.pageCount,
            includeNeighbors = includeNeighbors,
        )
    }
}

private fun heroLayerPages(
    currentPage: Int,
    pageCount: Int,
    includeNeighbors: Boolean,
): List<Int> {
    if (!includeNeighbors || pageCount == 1) return listOf(currentPage)

    val neighbors = listOf(currentPage - 1, currentPage + 1)
        .map { page -> page.coerceIn(0, pageCount - 1) }
        .filter { page -> page != currentPage }
        .distinct()
    return neighbors + currentPage
}

@Composable
private fun HeroDesktopContentLayers(
    items: List<MetaPreview>,
    pagerState: PagerState,
    layout: HomeHeroLayout,
    heroWidthPx: Float,
    onItemClick: ((MetaPreview) -> Unit)?,
    includePagerNeighbors: Boolean,
) {
    val layerPages = rememberHeroLayerPages(
        pagerState = pagerState,
        itemCount = items.size,
        includePagerNeighbors = includePagerNeighbors,
    )

    val allowsSpatialEffects = com.nuvio.app.core.ui.LocalUiMotion.current.allowsSpatialEffects
    val contentLiftPx = with(LocalDensity.current) { 18.dp.toPx() }
    layerPages.forEach { page ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .homeHeroContentTransition(
                    pageOffset = heroPageOffset(pagerState, page),
                    heroWidthPx = heroWidthPx,
                    contentLiftPx = contentLiftPx,
                    allowsSpatialEffects = allowsSpatialEffects,
                ),
        ) {
            DesktopHeroContentBlock(
                item = items[page % items.size],
                layout = layout,
                onItemClick = onItemClick,
            )
        }
    }
}

private fun Modifier.homeHeroContentTransition(
    pageOffset: Float,
    heroWidthPx: Float,
    contentLiftPx: Float,
    allowsSpatialEffects: Boolean,
): Modifier = graphicsLayer {
    val pageProgress = heroPageVisibility(pageOffset)
    val distanceFromActivePage = abs(pageOffset).coerceIn(0f, 1f)
    val contentScale = if (allowsSpatialEffects) 1f - (distanceFromActivePage * 0.018f) else 1f

    alpha = pageProgress
    translationX = if (allowsSpatialEffects) -pageOffset * heroWidthPx * HERO_CONTENT_PARALLAX else 0f
    translationY = if (allowsSpatialEffects) distanceFromActivePage * contentLiftPx else 0f
    scaleX = contentScale
    scaleY = contentScale
}

@Composable
private fun DefaultHomeHeroFrame(
    items: List<MetaPreview>,
    pagerState: PagerState,
    listState: LazyListState?,
    layout: HomeHeroLayout,
    heroWidthPx: Float,
    heroHeightPx: Float,
    stretchPx: () -> Float,
    includePagerNeighbors: Boolean,
    coroutineScope: CoroutineScope,
    onItemClick: ((MetaPreview) -> Unit)?,
    trailerPlaybackSource: TrailerPlaybackSource?,
    trailerSoundEnabled: Boolean,
    trailerStartPositionSeconds: Int,
) {
    var previewSoundEnabled by remember(trailerPlaybackSource, trailerSoundEnabled) { mutableStateOf(trailerSoundEnabled) }
    var previewPlaying by remember(trailerPlaybackSource) { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        HeroBackgroundLayers(
            items = items,
            pagerState = pagerState,
            listState = listState,
            layout = layout,
            heroWidthPx = heroWidthPx,
            heroHeightPx = heroHeightPx,
            stretchPx = stretchPx,
            includePagerNeighbors = includePagerNeighbors,
        )

        HomePosterHoverTrailer(
            playbackSource = trailerPlaybackSource,
            soundEnabled = previewSoundEnabled,
            startPositionSeconds = trailerStartPositionSeconds,
            onReady = { previewPlaying = true },
            onEnded = { previewPlaying = false },
            onError = { previewPlaying = false },
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.02f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.34f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.78f),
                        ),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.bottomFadeHeight)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    horizontal = layout.contentHorizontalPadding,
                    vertical = layout.contentVerticalPadding,
                ),
            horizontalAlignment = if (layout.isTablet) Alignment.Start else Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(layout.contentWidthFraction)
                    .widthIn(max = layout.contentMaxWidth),
                contentAlignment = if (layout.isTablet) Alignment.CenterStart else Alignment.BottomStart,
            ) {
                HeroContentLayers(
                    items = items,
                    pagerState = pagerState,
                    layout = layout,
                    heroWidthPx = heroWidthPx,
                    onItemClick = onItemClick,
                    includePagerNeighbors = includePagerNeighbors,
                )
            }

            if (!layout.isTablet) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    modifier = Modifier
                        .width(188.dp)
                        .height(48.dp)
                        .clickable(enabled = onItemClick != null) {
                            onItemClick?.invoke(currentHeroItem(items, pagerState))
                        },
                    color = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(40.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.home_view_details),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            HeroPageIndicatorRow(
                itemCount = items.size,
                pagerState = pagerState,
                coroutineScope = coroutineScope,
                modifier = Modifier.padding(top = if (layout.isTablet) 14.dp else 12.dp),
            )
        }
        HomePreviewAudioControl(
            visible = previewPlaying,
            soundEnabled = previewSoundEnabled,
            onToggleSound = { previewSoundEnabled = !previewSoundEnabled },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 88.dp, end = 24.dp),
        )
    }
}

@Composable
internal fun DesktopHomeHeroFrame(
    items: List<MetaPreview>,
    pagerState: PagerState,
    listState: LazyListState?,
    layout: HomeHeroLayout,
    heroWidthPx: Float,
    heroHeightPx: Float,
    stretchPx: () -> Float,
    includePagerNeighbors: Boolean,
    contentHorizontalPadding: Dp,
    coroutineScope: CoroutineScope,
    onItemClick: ((MetaPreview) -> Unit)?,
    trailerPlaybackSource: TrailerPlaybackSource?,
    trailerSoundEnabled: Boolean,
    trailerStartPositionSeconds: Int,
) {
    val colorScheme = MaterialTheme.colorScheme
    val opacity = NuvioTokens.Opacity
    val space = NuvioTokens.Space
    val backgroundColor = colorScheme.background
    var previewSoundEnabled by remember(trailerPlaybackSource, trailerSoundEnabled) { mutableStateOf(trailerSoundEnabled) }
    var previewPlaying by remember(trailerPlaybackSource) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
    ) {
        HeroBackgroundLayers(
            items = items,
            pagerState = pagerState,
            listState = listState,
            layout = layout,
            heroWidthPx = heroWidthPx,
            heroHeightPx = heroHeightPx,
            stretchPx = stretchPx,
            includePagerNeighbors = includePagerNeighbors,
            desktopFrame = true,
        )

        HomePosterHoverTrailer(
            playbackSource = trailerPlaybackSource,
            soundEnabled = previewSoundEnabled,
            startPositionSeconds = trailerStartPositionSeconds,
            onReady = { previewPlaying = true },
            onEnded = { previewPlaying = false },
            onError = { previewPlaying = false },
            // Windows embeds a heavyweight native video window: Compose text cannot
            // paint above it. Preserve title/actions and navigation outside its bounds.
            modifier = if (isWindows) {
                Modifier.align(Alignment.CenterEnd)
                    .padding(top = 144.dp, end = contentHorizontalPadding, bottom = 100.dp)
                    .fillMaxWidth(0.46f)
                    .aspectRatio(16f / 9f)
            } else Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.topFadeHeight)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = opacity.overlayHeavy),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.00f to backgroundColor.copy(alpha = 0.96f),
                            0.08f to backgroundColor.copy(alpha = 0.90f),
                            0.16f to backgroundColor.copy(alpha = 0.76f),
                            0.26f to backgroundColor.copy(alpha = 0.54f),
                            0.36f to backgroundColor.copy(alpha = 0.30f),
                            0.46f to backgroundColor.copy(alpha = 0.12f),
                            0.54f to Color.Transparent,
                        ),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.bottomFadeHeight)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0f),
                            backgroundColor,
                        ),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = layout.contentContainerMaxWidth)
                .fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(
                        start = contentHorizontalPadding,
                        end = space.s32,
                        top = 96.dp,
                        bottom = 100.dp,
                    )
                    .width((layout.contentContainerMaxWidth *
                        (if (isWindows) minOf(layout.contentWidthFraction, 0.49f) else layout.contentWidthFraction) -
                        contentHorizontalPadding - space.s32)
                        .coerceIn(if (isWindows) 240.dp else 320.dp, layout.contentMaxWidth)),
                contentAlignment = Alignment.BottomStart,
            ) {
                HeroDesktopContentLayers(
                    items = items,
                    pagerState = pagerState,
                    layout = layout,
                    heroWidthPx = heroWidthPx,
                    onItemClick = onItemClick,
                    includePagerNeighbors = includePagerNeighbors,
                )
            }

            if (isFullscreenActionSupported) {
                FullscreenActionButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(
                            top = space.s32,
                            end = contentHorizontalPadding,
                        ),
                    buttonSize = 48.dp,
                    iconSize = 24.dp,
                    containerColor = colorScheme.surfaceVariant.copy(alpha = 0.82f),
                    contentColor = colorScheme.onSurface,
                )
            }

            HomePreviewAudioControl(
                visible = previewPlaying,
                soundEnabled = previewSoundEnabled,
                onToggleSound = { previewSoundEnabled = !previewSoundEnabled },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 92.dp, end = contentHorizontalPadding),
            )
            DesktopSpotlightPager(
                items = items,
                pagerState = pagerState,
                coroutineScope = coroutineScope,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        start = contentHorizontalPadding,
                        end = contentHorizontalPadding,
                        bottom = 28.dp,
                    ).widthIn(max = 680.dp),
            )
        }
    }
}

@Composable
internal fun DesktopSpotlightPager(
    items: List<MetaPreview>,
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    if (items.size <= 1) return
    val colors = MaterialTheme.colorScheme
    val activeIndex = pagerState.settledPage % items.size
    val rowState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(activeIndex) { rowState.scrollToItem(activeIndex) }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("${activeIndex + 1} / ${items.size}", style = MaterialTheme.typography.labelMedium,
            color = colors.onBackground.copy(alpha = 0.82f))
        LazyRow(
            modifier = Modifier.weight(1f, fill = false), state = rowState,
            horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(items, key = { index, _ -> index }) { index, item ->
                val isActive = index == activeIndex
                val targetPage = heroPageForItem(pagerState.currentPage, index, items.size)
                Box(Modifier.size(width = 36.dp, height = 28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { coroutineScope.launch { pagerState.animateScrollToPage(targetPage) } }
                    .semantics { selected = isActive; contentDescription = item.name },
                    contentAlignment = Alignment.Center) {
                    Box(Modifier.clip(CircleShape).background(colors.onBackground)
                        .graphicsLayer {
                            val fraction = heroPageVisibility(pagerState, targetPage)
                            alpha = 0.35f + (0.57f * fraction)
                        }
                        .heroPageIndicatorSize(pagerState, targetPage))
                }
            }
        }
    }
}

@Composable
private fun HeroPageIndicatorRow(
    itemCount: Int,
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    if (itemCount <= 1) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(itemCount) { index ->
            val page = heroPageForItem(pagerState.currentPage, index, itemCount)
            val activeFraction = heroPageVisibility(pagerState, page)
            Box(
                modifier = Modifier
                    .clickable {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(heroPageForItem(pagerState.currentPage, index, itemCount))
                        }
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground)
                    .graphicsLayer {
                        alpha = 0.35f + (0.57f * activeFraction)
                    }
                    .width(8.dp + (24.dp * activeFraction))
                    .height(8.dp),
            )
        }
    }
}

internal fun heroPageForItem(currentPage: Int, itemIndex: Int, itemCount: Int): Int {
    val page = currentPage.toLong() - currentPage % itemCount + itemIndex
    return listOf(page - itemCount, page, page + itemCount)
        .filter { it in 0L until Int.MAX_VALUE.toLong() }
        .minBy { abs(it - currentPage) }
        .toInt()
}

private fun heroPageOffset(
    pagerState: PagerState,
    page: Int,
): Float = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction

private fun heroPageVisibility(
    pagerState: PagerState,
    page: Int,
): Float = heroPageVisibility(heroPageOffset(pagerState, page))

private fun heroPageVisibility(pageOffset: Float): Float = (1f - abs(pageOffset)).coerceIn(0f, 1f)

private fun currentHeroItem(
    items: List<MetaPreview>,
    pagerState: PagerState,
): MetaPreview {
    val currentPage = pagerState.currentPage
    val currentVisiblePages = heroLayerPages(
        currentPage = currentPage,
        pageCount = pagerState.pageCount,
        includeNeighbors = true,
    )
    val selectedPage = currentVisiblePages.maxBy { page ->
        heroPageVisibility(pagerState, page)
    }
    return items[selectedPage % items.size]
}

@Composable
private fun HeroPageIndicatorDot(
    pagerState: PagerState,
    page: Int,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onBackground)
            .graphicsLayer {
                val activeFraction = heroPageVisibility(pagerState, page)
                alpha = 0.35f + (0.57f * activeFraction)
            }
            .heroPageIndicatorSize(pagerState = pagerState, page = page),
    )
}

private fun Modifier.heroPageIndicatorSize(
    pagerState: PagerState,
    page: Int,
): Modifier = layout { measurable, constraints ->
    val activeFraction = heroPageVisibility(pagerState, page)
    val widthPx = (8.dp.toPx() + (24.dp.toPx() * activeFraction)).roundToInt()
    val heightPx = 8.dp.roundToPx()
    val constrainedWidth = widthPx.coerceIn(constraints.minWidth, constraints.maxWidth)
    val constrainedHeight = heightPx.coerceIn(constraints.minHeight, constraints.maxHeight)
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constrainedWidth,
            maxWidth = constrainedWidth,
            minHeight = constrainedHeight,
            maxHeight = constrainedHeight,
        ),
    )

    layout(constrainedWidth, constrainedHeight) {
        placeable.place(0, 0)
    }
}

@Composable
fun HomeHeroReservedSpace(
    modifier: Modifier = Modifier,
    viewportHeight: Dp? = null,
    mobileBelowSectionHeightHint: Dp? = null,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)),
    ) {
        val layout = homeHeroLayout(
            maxWidthDp = maxWidth.value,
            viewportHeightDp = viewportHeight?.value,
            mobileBelowSectionHeightHintDp = mobileBelowSectionHeightHint?.value,
            preferDesktopLayout = isDesktop,
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.heroHeight),
        )
    }
}

@Composable
private fun HeroContentBlock(
    item: MetaPreview,
    layout: HomeHeroLayout,
    onItemClick: ((MetaPreview) -> Unit)?,
) {
    var logoLoadError by remember(item.type, item.id, item.logo) {
        mutableStateOf(false)
    }
    val logoUrl = item.logo?.takeIf { it.isNotBlank() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (layout.isTablet) Alignment.Start else Alignment.CenterHorizontally,
    ) {
        if (logoUrl != null && !logoLoadError) {
            AsyncImage(
                model = logoUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxWidth(layout.logoWidthFraction)
                    .aspectRatio(2.6f)
                    .clickable(enabled = onItemClick != null) {
                        onItemClick?.invoke(item)
                    },
                alignment = if (layout.isTablet) Alignment.CenterStart else Alignment.Center,
                contentScale = ContentScale.Fit,
                onError = { logoLoadError = true },
            )
        } else {
            Text(
                text = item.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = onItemClick != null) {
                        onItemClick?.invoke(item)
                    },
                style = if (layout.isTablet) {
                    MaterialTheme.typography.displaySmall
                } else {
                    MaterialTheme.typography.displaySmall
                },
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Black,
                textAlign = if (layout.isTablet) TextAlign.Start else TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (layout.isTablet) {
                Arrangement.spacedBy(8.dp, Alignment.Start)
            } else {
                Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeroMetaText(text = item.type.replaceFirstChar(Char::uppercase))
            item.genres.firstOrNull()?.let { genre ->
                HeroMetaDot()
                HeroMetaText(text = genre)
            }
            item.releaseInfo?.takeIf { it.isNotBlank() }?.let { info ->
                HeroMetaDot()
                HeroMetaText(text = formatReleaseDateForDisplay(info))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun DesktopHeroContentBlock(
    item: MetaPreview,
    layout: HomeHeroLayout,
    onItemClick: ((MetaPreview) -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    val compact = layout.heroHeight < 620.dp
    val titleSize = when {
        layout.contentContainerMaxWidth >= 2800.dp -> 88.sp
        layout.contentContainerMaxWidth >= 1800.dp -> 72.sp
        else -> 50.sp
    }
    var logoLoadError by remember(item.type, item.id, item.logo) { mutableStateOf(false) }
    val logoUrl = item.logo?.takeIf(String::isNotBlank)
    val metadata = remember(item.type, item.releaseInfo, item.genres, compact) {
        buildList {
            item.releaseInfo?.takeIf(String::isNotBlank)?.let { add(formatReleaseDateForDisplay(it)) }
            addAll(item.genres.filter(String::isNotBlank).take(if (compact) 1 else 2))
        }.joinToString("  ·  ")
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 24.dp)) {
        if (!compact) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.width(4.dp).height(20.dp).background(colors.primary, RoundedCornerShape(2.dp)))
            Text(stringResource(Res.string.home_spotlight), style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant, letterSpacing = 2.sp)
        }
        if (logoUrl != null && !logoLoadError) {
            AsyncImage(model = logoUrl, contentDescription = item.name,
                modifier = Modifier.fillMaxWidth(0.90f).height(if (compact) 112.dp else 168.dp),
                contentScale = ContentScale.Fit, alignment = Alignment.CenterStart,
                onError = { logoLoadError = true })
        } else {
            Text(item.name, style = MaterialTheme.typography.displayLarge.copy(fontSize = titleSize,
                lineHeight = titleSize * 1.05f, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
                color = colors.onBackground, maxLines = if (compact) 2 else 3, overflow = TextOverflow.Ellipsis)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item.imdbRating?.toFloatOrNull()?.takeIf { it.isFinite() && it in 0f..10f }?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(color = Color(0xFFF5CF68), contentColor = Color(0xFF141414), shape = RoundedCornerShape(4.dp)) {
                        Text("IMDb", Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(item.imdbRating, style = MaterialTheme.typography.titleSmall, color = colors.onBackground)
                }
            }
            if (metadata.isNotBlank()) Text(metadata, style = MaterialTheme.typography.titleSmall,
                color = colors.onBackground.copy(alpha = 0.86f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        item.description?.takeIf(String::isNotBlank)?.let {
            Text(it, modifier = Modifier.widthIn(max = 720.dp), style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = if (compact) 24.sp else 26.sp),
                color = colors.onBackground.copy(alpha = 0.90f), maxLines = if (compact) 2 else 3,
                overflow = TextOverflow.Ellipsis)
        }
        if (onItemClick != null) {
            Button(onClick = { onItemClick(item) }, modifier = Modifier
                .align(Alignment.Start)
                .width(188.dp)
                .height(if (compact) 48.dp else 52.dp),
                shape = RoundedCornerShape(26.dp), colors = ButtonDefaults.buttonColors(
                    containerColor = colors.onBackground, contentColor = colors.background)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.home_view_details), style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}
private fun desktopHeroLogoWidthFraction(layout: HomeHeroLayout): Float =
    when {
        layout.contentMaxWidth >= 640.dp -> 0.74f
        layout.contentMaxWidth >= 520.dp -> 0.74f
        else -> 0.8f
    }

private fun desktopHeroLogoSlotHeight(layout: HomeHeroLayout): Dp =
    when {
        layout.contentMaxWidth >= 640.dp -> 120.dp
        layout.contentMaxWidth >= 520.dp -> 112.dp
        else -> 104.dp
    }

private fun desktopHeroGenreText(item: MetaPreview): String =
    item.genres
        .take(3)
        .joinToString(" • ")
        .ifBlank { item.type.replaceFirstChar(Char::uppercase) }

@Composable
private fun HeroMetaText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

internal fun homeHeroLayout(
    maxWidthDp: Float,
    viewportHeightDp: Float? = null,
    mobileBelowSectionHeightHintDp: Float? = null,
    preferDesktopLayout: Boolean = false,
): HomeHeroLayout {
    if (preferDesktopLayout) {
        val heroHeight = desktopHeroHeight(
            maxWidthDp = maxWidthDp,
            viewportHeightDp = viewportHeightDp,
        )
        val ultrawideProgress = ultrawideViewportProgress(
            widthDp = maxWidthDp,
            heightDp = viewportHeightDp,
        )

        val standardHorizontalPadding = homeSectionHorizontalPaddingForWidth(maxWidthDp).value

        return HomeHeroLayout(
            isTablet = true,
            heroHeight = heroHeight,
            contentMaxWidth = 1160.dp,
            contentContainerMaxWidth = maxWidthDp.dp,
            contentWidthFraction = if (maxWidthDp < 1000f) 0.78f else 0.56f,
            contentHorizontalPadding = lerp(
                start = standardHorizontalPadding,
                stop = DESKTOP_HERO_ULTRAWIDE_HORIZONTAL_PADDING_DP,
                fraction = ultrawideProgress,
            ).dp,
            contentVerticalPadding = lerp(
                start = 40f,
                stop = DESKTOP_HERO_ULTRAWIDE_BOTTOM_PADDING_DP,
                fraction = ultrawideProgress,
            ).dp,
            topFadeHeight = DESKTOP_HERO_TOP_FADE_HEIGHT_DP.dp,
            bottomFadeHeight = DESKTOP_HERO_BOTTOM_FADE_HEIGHT_DP.dp,
            logoWidthFraction = 0.74f,
            backgroundMotionStrength = 1f - ultrawideProgress,
        )
    }

    return when {
        maxWidthDp >= 1200f -> HomeHeroLayout(
            isTablet = true,
            heroHeight = (maxWidthDp * 0.42f).dp.coerceIn(360.dp, 440.dp),
            contentMaxWidth = 640.dp,
            contentContainerMaxWidth = maxWidthDp.dp,
            contentWidthFraction = 0.56f,
            contentHorizontalPadding = 56.dp,
            contentVerticalPadding = 22.dp,
            topFadeHeight = 0.dp,
            bottomFadeHeight = 190.dp,
            logoWidthFraction = 0.58f,
            backgroundMotionStrength = 1f,
        )
        maxWidthDp >= 840f -> HomeHeroLayout(
            isTablet = true,
            heroHeight = (maxWidthDp * 0.46f).dp.coerceIn(340.dp, 420.dp),
            contentMaxWidth = 560.dp,
            contentContainerMaxWidth = maxWidthDp.dp,
            contentWidthFraction = 0.62f,
            contentHorizontalPadding = 40.dp,
            contentVerticalPadding = 20.dp,
            topFadeHeight = 0.dp,
            bottomFadeHeight = 180.dp,
            logoWidthFraction = 0.56f,
            backgroundMotionStrength = 1f,
        )
        maxWidthDp >= 600f -> HomeHeroLayout(
            isTablet = true,
            heroHeight = (maxWidthDp * 0.58f).dp.coerceIn(320.dp, 380.dp),
            contentMaxWidth = 520.dp,
            contentContainerMaxWidth = maxWidthDp.dp,
            contentWidthFraction = 0.72f,
            contentHorizontalPadding = 32.dp,
            contentVerticalPadding = 18.dp,
            topFadeHeight = 0.dp,
            bottomFadeHeight = 170.dp,
            logoWidthFraction = 0.54f,
            backgroundMotionStrength = 1f,
        )
        else -> HomeHeroLayout(
            isTablet = false,
            heroHeight = mobileHeroHeight(
                maxWidthDp = maxWidthDp,
                viewportHeightDp = viewportHeightDp,
                mobileBelowSectionHeightHintDp = mobileBelowSectionHeightHintDp,
            ),
            contentMaxWidth = 480.dp,
            contentContainerMaxWidth = maxWidthDp.dp,
            contentWidthFraction = 1f,
            contentHorizontalPadding = 24.dp,
            contentVerticalPadding = 16.dp,
            topFadeHeight = 0.dp,
            bottomFadeHeight = 220.dp,
            logoWidthFraction = 0.62f,
            backgroundMotionStrength = 1f,
        )
    }
}

internal fun shouldUseCinematicHomeLayout(
    isDesktop: Boolean,
    isTelevision: Boolean,
    maxWidthDp: Float,
): Boolean = isDesktop || (isTelevision && maxWidthDp >= CINEMATIC_TV_MIN_WIDTH_DP)

internal fun homeHeroRailOverlapForViewport(viewportHeightDp: Float): Dp =
    (viewportHeightDp * 0.11f).coerceIn(72f, 128f).dp

private fun desktopHeroHeight(
    maxWidthDp: Float,
    viewportHeightDp: Float?,
): Dp {
    val baselineHeight = ((viewportHeightDp ?: (maxWidthDp * 0.56f)) * CINEMATIC_DESKTOP_HERO_VIEWPORT_RATIO)
        .dp.coerceIn(560.dp, 1280.dp)
    val viewportHeight = viewportHeightDp ?: return baselineHeight
    val ultrawideProgress = ultrawideViewportProgress(
        widthDp = maxWidthDp,
        heightDp = viewportHeight,
    )
    if (ultrawideProgress <= 0f) return baselineHeight

    val ultrawideHeight = (viewportHeight * ULTRAWIDE_HERO_VIEWPORT_HEIGHT_RATIO).dp
        .coerceAtLeast(baselineHeight)
    return (
        baselineHeight.value +
            (ultrawideHeight.value - baselineHeight.value) * ultrawideProgress
        ).dp
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

private fun mobileHeroHeight(
    maxWidthDp: Float,
    viewportHeightDp: Float?,
    mobileBelowSectionHeightHintDp: Float?,
): Dp {
    val viewportDrivenHeight = viewportHeightDp?.let { (it * MOBILE_HERO_VIEWPORT_RATIO).dp }
    val widthFallbackHeight = (maxWidthDp * 1.16f).dp
    val baseHeight = if (mobileBelowSectionHeightHintDp == null) {
        viewportDrivenHeight?.coerceAtMost(widthFallbackHeight) ?: widthFallbackHeight
    } else {
        viewportDrivenHeight ?: widthFallbackHeight
    }

    val cappedHeight = if (viewportHeightDp != null && mobileBelowSectionHeightHintDp != null) {
        val maxAllowedFromViewport = (viewportHeightDp - mobileBelowSectionHeightHintDp).dp
        baseHeight.coerceAtMost(maxAllowedFromViewport)
    } else {
        baseHeight
    }

    return if (viewportHeightDp != null && mobileBelowSectionHeightHintDp != null) {
        cappedHeight.coerceIn(0.dp, MOBILE_HERO_MAX_HEIGHT_DP.dp)
    } else {
        cappedHeight.coerceIn(MOBILE_HERO_MIN_HEIGHT_DP.dp, MOBILE_HERO_MAX_HEIGHT_DP.dp)
    }
}

@Composable
private fun HeroMetaDot() {
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)),
    )
}

private fun heroScrollOffsetPx(
    listState: LazyListState?,
    heroHeightPx: Float,
): Float = when {
    listState == null -> 0f
    listState.firstVisibleItemIndex > 0 -> heroHeightPx
    else -> listState.firstVisibleItemScrollOffset.toFloat()
}

private fun heroBackgroundScrollScale(scrollOffsetPx: Float): Float {
    val scaleIncrease = if (scrollOffsetPx < 0f) {
        abs(scrollOffsetPx) * HERO_SCROLL_UP_SCALE_MULTIPLIER
    } else {
        scrollOffsetPx * HERO_SCROLL_DOWN_SCALE_MULTIPLIER
    }
    return (1f + scaleIncrease).coerceAtMost(HERO_SCROLL_MAX_SCALE)
}

private fun heroBackgroundScrollTranslationY(scrollOffsetPx: Float): Float {
    return scrollOffsetPx * HERO_SCROLL_PARALLAX
}

private fun heroDesktopBackgroundScrollTranslationY(scrollOffsetPx: Float): Float {
    return scrollOffsetPx * DESKTOP_HERO_SCROLL_PARALLAX
}

private fun Modifier.homeHeroPagerGesture(
    pagerState: PagerState,
    itemCount: Int,
    coroutineScope: CoroutineScope,
    onDragActiveChange: (Boolean) -> Unit,
): Modifier {
    if (itemCount <= 1) return this

    return pointerInput(pagerState, itemCount) {
        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial)
            val widthPx = size.width.toFloat().takeIf { it > 0f } ?: return@awaitEachGesture
            val velocityTracker = VelocityTracker().apply {
                addPosition(down.uptimeMillis, down.position)
            }
            val startPage = pagerState.currentPage
            var totalDx = 0f
            var totalDy = 0f
            var dragging = false
            var settleAnimationStarted = false

            try {
                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    velocityTracker.addPosition(change.uptimeMillis, change.position)

                    if (!change.pressed) {
                        if (dragging) {
                            val targetPage = resolveHeroTargetPage(
                                startPage = startPage,
                                pageCount = pagerState.pageCount,
                                totalDx = totalDx,
                                velocityX = velocityTracker.calculateVelocity().x,
                                widthPx = widthPx,
                            )
                            settleAnimationStarted = true
                            coroutineScope.launch {
                                try {
                                    pagerState.animateScrollToPage(targetPage)
                                } finally {
                                    onDragActiveChange(false)
                                }
                            }
                        }
                        break
                    }

                    val delta = change.position - change.previousPosition
                    totalDx += delta.x
                    totalDy += delta.y

                    if (!dragging) {
                        val horizontalDrag =
                            abs(totalDx) > viewConfiguration.touchSlop && abs(totalDx) > abs(totalDy)
                        val verticalDrag =
                            abs(totalDy) > viewConfiguration.touchSlop && abs(totalDy) > abs(totalDx)

                        when {
                            verticalDrag -> break
                            horizontalDrag -> {
                                dragging = true
                                onDragActiveChange(true)
                            }
                            else -> continue
                        }
                    }

                    pagerState.dispatchRawDelta(-delta.x)
                    change.consume()
                }
            } finally {
                if (dragging && !settleAnimationStarted) {
                    onDragActiveChange(false)
                }
            }
        }
    }
}

private fun resolveHeroTargetPage(
    startPage: Int,
    pageCount: Int,
    totalDx: Float,
    velocityX: Float,
    widthPx: Float,
): Int {
    val thresholdPassed = abs(totalDx) > widthPx * HERO_SWIPE_THRESHOLD_FRACTION ||
        abs(velocityX) > HERO_SWIPE_VELOCITY_THRESHOLD
    if (!thresholdPassed) return startPage

    val currentPage = startPage.coerceIn(0, pageCount - 1)
    return when {
        totalDx > 0f -> (currentPage - 1).coerceAtLeast(0)
        totalDx < 0f -> (currentPage + 1).coerceAtMost(pageCount - 1)
        else -> currentPage
    }
}
