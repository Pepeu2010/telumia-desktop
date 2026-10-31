package com.nuvio.app.features.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.ui.NuvioAsyncImage
import com.nuvio.app.core.ui.NuvioPosterWatchedOverlay
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.LocalUiMotion
import com.nuvio.app.core.ui.LocalNativeVideoHoverInteractionSource
import com.nuvio.app.core.ui.PosterCardStyleUiState
import com.nuvio.app.core.ui.PosterZoomOverlayCoordinator
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.core.ui.secondaryClick
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.details.components.DetailIconAction
import com.nuvio.app.features.library.LibraryRepository
import com.nuvio.app.features.library.PendingTrackingMembershipRemoval
import com.nuvio.app.features.library.TrackingMembershipRemovalConfirmationHost
import com.nuvio.app.features.library.executeTrackingMembershipOperation
import com.nuvio.app.features.library.showTrackingMembershipRewriteFeedback
import com.nuvio.app.features.library.toLibraryItem
import com.nuvio.app.features.trailer.TrailerPlaybackSource
import com.nuvio.app.features.tracking.TrackingMembershipApplyResult
import com.nuvio.app.features.tracking.TrackingProviderId
import com.nuvio.app.features.watching.application.WatchingActions
import com.nuvio.app.isDesktop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import kotlin.math.roundToInt
import kotlin.coroutines.coroutineContext
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.hero_add_to_library
import nuvio.composeapp.generated.resources.hero_mark_unwatched
import nuvio.composeapp.generated.resources.hero_mark_watched
import nuvio.composeapp.generated.resources.hero_remove_from_library
import nuvio.composeapp.generated.resources.home_view_details
import nuvio.composeapp.generated.resources.poster_logo_content_description
import nuvio.composeapp.generated.resources.tracking_lists_update_failed
import org.jetbrains.compose.resources.stringResource

private const val HoverPreviewCloseDelayMillis = 120L
private const val HoverPreviewEnterDurationMillis = 260
private const val HoverPreviewExitDurationMillis = 170
private const val HoverTrailerExtractionDebounceMillis = 1_000L
private val HoverPreviewWidth = 420.dp

private class HomePosterHoverPreviewPortalEntry(
    val owner: Any,
    val positionProvider: PopupPositionProvider,
    anchorBounds: IntRect,
    content: @Composable () -> Unit,
) {
    var anchorBounds by mutableStateOf(anchorBounds)
    var content by mutableStateOf(content)
}

private object HomePosterHoverPreviewPortal {
    var activeEntry by mutableStateOf<HomePosterHoverPreviewPortalEntry?>(null)
        private set

    fun show(entry: HomePosterHoverPreviewPortalEntry) {
        activeEntry = entry
    }

    fun clear(owner: Any) {
        if (activeEntry?.owner === owner) activeEntry = null
    }
}

/** Renders the active Windows hover card in the main window, where SwingPanel has an interop host. */
@Composable
internal fun HomePosterHoverPreviewOverlayHost() {
    val entry = HomePosterHoverPreviewPortal.activeEntry ?: return
    SubcomposeLayout(Modifier.fillMaxSize().zIndex(1f)) { constraints ->
        if (!constraints.hasBoundedWidth || !constraints.hasBoundedHeight) {
            return@SubcomposeLayout layout(0, 0) {}
        }
        val popup = subcompose(entry.owner) { entry.content() }
            .single()
            .measure(Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight))
        val windowSize = IntSize(constraints.maxWidth, constraints.maxHeight)
        val popupSize = IntSize(popup.width, popup.height)
        val position = entry.positionProvider.calculatePosition(
            anchorBounds = entry.anchorBounds,
            windowSize = windowSize,
            layoutDirection = layoutDirection,
            popupContentSize = popupSize,
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            popup.place(position.x, position.y)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun HomePosterHoverPreview(
    item: MetaPreview,
    isWatched: Boolean,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    previewSettings: PosterCardStyleUiState? = null,
    trailerPreviewSupported: Boolean = AppFeaturePolicy.hoverTrailerPlaybackSupported,
    anchorInteractionSourceOverride: MutableInteractionSource? = null,
    trailerResolver: suspend (MetaPreview) -> TrailerPlaybackSource? = ::resolveHomePosterHoverTrailerPlaybackSource,
    onTrailerReady: () -> Unit = {},
    onTrailerError: () -> Unit = {},
    content: @Composable (Modifier) -> Unit,
) {
    if (!isDesktop) {
        content(modifier)
        return
    }

    val posterCardStyle = previewSettings ?: rememberPosterCardStyleUiState()
    val motion = LocalUiMotion.current
    val trailerPlaybackEnabled = trailerPreviewSupported && posterCardStyle.hoverPreviewTrailerEnabled
    if (!posterCardStyle.hoverPreviewEnabled) {
        content(modifier)
        return
    }

    if (PosterZoomOverlayCoordinator.isVisible) {
        content(modifier)
        return
    }

    val anchorInteractionSource = remember(anchorInteractionSourceOverride) {
        anchorInteractionSourceOverride ?: MutableInteractionSource()
    }
    val previewInteractionSource = remember { MutableInteractionSource() }
    val ownershipToken = remember(item.type, item.id) { Any() }
    val activeOwner by homePosterPreviewOwnership.active.collectAsState()
    val ownsPreview = activeOwner === ownershipToken
    val currentTrailerResolver by rememberUpdatedState(trailerResolver)
    DisposableEffect(ownershipToken) {
        onDispose {
            HomePosterHoverPreviewPortal.clear(ownershipToken)
            homePosterPreviewOwnership.release(ownershipToken)
        }
    }
    val anchorHovered by anchorInteractionSource.collectIsHoveredAsState()
    val previewHovered by previewInteractionSource.collectIsHoveredAsState()
    var previewVisible by remember(item.type, item.id) { mutableStateOf(false) }
    var popupMounted by remember(item.type, item.id) { mutableStateOf(false) }
    var previewDismissedByScroll by remember(item.type, item.id) { mutableStateOf(false) }
    var trailerPlaybackSource by remember(item.type, item.id) {
        mutableStateOf<TrailerPlaybackSource?>(null)
    }
    var previewIsSaved by remember(item.type, item.id) { mutableStateOf(false) }
    var pendingTrackingRemoval by remember(item.type, item.id) {
        mutableStateOf<PendingTrackingMembershipRemoval?>(null)
    }
    val actionScope = rememberCoroutineScope()
    val libraryItem = remember(item) { item.toLibraryItem(savedAtEpochMs = 0L) }
    val trackingListsUpdateFailedMessage = stringResource(Res.string.tracking_lists_update_failed)
    val density = LocalDensity.current
    val positionProvider = remember(density) {
        HomePosterPreviewPositionProvider(
            edgeMarginPx = with(density) { NuvioTokens.Space.s16.roundToPx() },
            topOverlapPx = with(density) { NuvioTokens.Space.s24.roundToPx() },
        )
    }
    val useRootOverlay = AppFeaturePolicy.hoverPreviewUsesRootOverlay && trailerPlaybackEnabled
    var anchorBounds by remember(ownershipToken) { mutableStateOf<IntRect?>(null) }
    val portalEntry = remember(ownershipToken, positionProvider, useRootOverlay) {
        if (useRootOverlay) {
            HomePosterHoverPreviewPortalEntry(
                owner = ownershipToken,
                positionProvider = positionProvider,
                anchorBounds = IntRect(0, 0, 0, 0),
                content = {},
            )
        } else {
            null
        }
    }

    LaunchedEffect(
        anchorHovered,
        previewHovered,
        posterCardStyle.hoverPreviewOpenDelayMillis,
        previewDismissedByScroll,
        motion,
        ownershipToken,
    ) {
        if (previewDismissedByScroll) {
            if (!anchorHovered && !previewHovered) {
                previewDismissedByScroll = false
            }
            return@LaunchedEffect
        }
        if (anchorHovered || previewHovered) {
            if (!popupMounted) {
                delay(posterCardStyle.hoverPreviewOpenDelayMillis.toLong())
                homePosterPreviewOwnership.claim(ownershipToken)
                popupMounted = true
                withFrameNanos { }
            }
            previewVisible = true
        } else if (popupMounted) {
            delay(HoverPreviewCloseDelayMillis)
            previewVisible = false
            delay(motion.durationMillis(HoverPreviewExitDurationMillis).toLong())
            popupMounted = false
            homePosterPreviewOwnership.release(ownershipToken)
        }
    }

    LaunchedEffect(ownsPreview) {
        if (!ownsPreview) {
            previewVisible = false
            popupMounted = false
        }
    }

    // Keep the decoder through the card's close grace while pointer ownership moves to AWT.
    LaunchedEffect(ownsPreview, previewVisible, trailerPlaybackEnabled, item.type, item.id) {
        trailerPlaybackSource = null
        if (!ownsPreview || !previewVisible || !trailerPlaybackEnabled) return@LaunchedEffect
        delay(HoverTrailerExtractionDebounceMillis)
        val source = resolveBoundedHoverPreview { currentTrailerResolver(item) }
        coroutineContext.ensureActive()
        if (homePosterPreviewOwnership.active.value === ownershipToken) {
            trailerPlaybackSource = source
        }
    }

    LaunchedEffect(popupMounted, item.type, item.id) {
        if (popupMounted) {
            previewIsSaved = LibraryRepository.isSaved(item.id, item.type)
        }
    }

    val onWatchedClick = remember(item) {
        {
            actionScope.launch {
                WatchingActions.togglePosterWatched(item)
            }
            Unit
        }
    }
    val onSaveClick = remember(libraryItem, trackingListsUpdateFailedMessage) {
        {
            actionScope.launch {
                val toggleMembership: suspend (Set<TrackingProviderId>) -> TrackingMembershipApplyResult =
                    { confirmedProviders ->
                        LibraryRepository.toggleSaved(
                            item = libraryItem,
                            confirmedRemovalProviders = confirmedProviders,
                        )
                    }
                val completeUpdate: suspend (TrackingMembershipApplyResult) -> Unit = { result ->
                    showTrackingMembershipRewriteFeedback(result)
                    previewIsSaved = LibraryRepository.isSaved(item.id, item.type)
                }
                val showFailure: suspend (Throwable) -> Unit = { error ->
                    com.nuvio.app.core.ui.NuvioToastController.show(
                        error.message ?: trackingListsUpdateFailedMessage,
                    )
                }
                executeTrackingMembershipOperation(
                    operation = { toggleMembership(emptySet()) },
                    onSuccess = { result ->
                        if (result.requiresRemovalConfirmation) {
                            pendingTrackingRemoval = PendingTrackingMembershipRemoval(
                                itemTitle = libraryItem.name,
                                confirmations = result.requiredRemovalConfirmations,
                                retry = toggleMembership,
                                onApplied = completeUpdate,
                                onFailure = showFailure,
                            )
                        } else {
                            completeUpdate(result)
                        }
                    },
                    onFailure = showFailure,
                )
            }
            Unit
        }
    }

    val previewContent: @Composable () -> Unit = {
        AnimatedVisibility(
            visible = previewVisible,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = motion.durationMillis(HoverPreviewEnterDurationMillis),
                    easing = NuvioTokens.Motion.decelerate,
                ),
            ) + scaleIn(
                animationSpec = tween(
                    durationMillis = motion.durationMillis(HoverPreviewEnterDurationMillis),
                    easing = NuvioTokens.Motion.emphasized,
                ),
                initialScale = motion.scale(0.9f),
            ) + slideInVertically(
                animationSpec = tween(
                    durationMillis = motion.durationMillis(HoverPreviewEnterDurationMillis),
                    easing = NuvioTokens.Motion.decelerate,
                ),
                initialOffsetY = { height -> if (motion.allowsSpatialEffects) height / 18 else 0 },
            ),
            exit = fadeOut(animationSpec = tween(motion.durationMillis(HoverPreviewExitDurationMillis))) +
                scaleOut(
                    animationSpec = tween(motion.durationMillis(HoverPreviewExitDurationMillis)),
                    targetScale = motion.scale(0.97f),
                ) + slideOutVertically(
                    animationSpec = tween(motion.durationMillis(HoverPreviewExitDurationMillis)),
                    targetOffsetY = { height -> if (motion.allowsSpatialEffects) height / 32 else 0 },
                ),
        ) {
            CompositionLocalProvider(LocalNativeVideoHoverInteractionSource provides previewInteractionSource) {
                HomePosterPreviewCard(
                    item = item,
                    isWatched = isWatched,
                    isSaved = previewIsSaved,
                    trailerEnabled = trailerPlaybackEnabled,
                    trailerSoundEnabled = posterCardStyle.hoverPreviewTrailerSoundEnabled,
                    trailerStartSeconds = posterCardStyle.hoverPreviewTrailerStartSeconds,
                    trailerPlaybackSource = trailerPlaybackSource.takeIf { ownsPreview && previewVisible },
                    onTrailerReady = onTrailerReady,
                    onTrailerError = onTrailerError,
                    modifier = Modifier
                        .testTag("home-hover-preview")
                        .hoverable(previewInteractionSource)
                        .onPointerEvent(PointerEventType.Scroll) {
                            previewDismissedByScroll = true
                            previewVisible = false
                            popupMounted = false
                            homePosterPreviewOwnership.release(ownershipToken)
                        },
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onWatchedClick = onWatchedClick,
                    onSaveClick = onSaveClick,
                )
            }
        }
    }
    val latestPreviewContent = rememberUpdatedState(previewContent)
    val portalContent: @Composable () -> Unit = remember(ownershipToken) {
        { latestPreviewContent.value() }
    }
    SideEffect {
        if (portalEntry != null && popupMounted && ownsPreview && anchorBounds != null) {
            portalEntry.content = portalContent
            portalEntry.anchorBounds = anchorBounds!!
            HomePosterHoverPreviewPortal.show(portalEntry)
        } else {
            HomePosterHoverPreviewPortal.clear(ownershipToken)
        }
    }

    Box(modifier = modifier) {
        content(
            Modifier
                .hoverable(anchorInteractionSource)
                .onGloballyPositioned { coordinates ->
                    if (useRootOverlay) {
                        val bounds = coordinates.boundsInRoot()
                        anchorBounds = IntRect(
                            left = bounds.left.roundToInt(),
                            top = bounds.top.roundToInt(),
                            right = bounds.right.roundToInt(),
                            bottom = bounds.bottom.roundToInt(),
                        )
                    }
                },
        )

        if (popupMounted && !useRootOverlay) {
            Popup(
                popupPositionProvider = positionProvider,
                properties = PopupProperties(focusable = false),
            ) {
                previewContent()
            }
        }
    }

    pendingTrackingRemoval?.let { pending ->
        TrackingMembershipRemovalConfirmationHost(
            pending = pending,
            onPendingChange = { pendingTrackingRemoval = it },
        )
    }
}

@Composable
private fun HomePosterPreviewCard(
    item: MetaPreview,
    isWatched: Boolean,
    isSaved: Boolean,
    trailerEnabled: Boolean,
    trailerSoundEnabled: Boolean,
    trailerStartSeconds: Int,
    trailerPlaybackSource: TrailerPlaybackSource?,
    onTrailerReady: () -> Unit,
    onTrailerError: () -> Unit,
    modifier: Modifier,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    onWatchedClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val shape = RoundedCornerShape(NuvioTokens.Radius.xl)
    val previewBackground = tokens.colors.background
    val clickInteractionSource = remember { MutableInteractionSource() }
    var logoLoadError by remember(item.type, item.id, item.logo) { mutableStateOf(false) }
    val logoUrl = item.logo?.takeIf { it.isNotBlank() && !logoLoadError }

    Column(
        modifier = modifier
            .width(HoverPreviewWidth)
            .shadow(tokens.elevation.overlay, shape)
            .clip(shape)
            .background(previewBackground)
            .border(tokens.borders.hairline, tokens.colors.borderStrong, shape)
            .clickable(
                interactionSource = clickInteractionSource,
                indication = null,
                enabled = onClick != null,
            ) {
                onClick?.invoke()
            }
            .secondaryClick(onLongClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(tokens.colors.surface),
        ) {
            val artworkUrl = item.banner ?: item.poster
            if (!artworkUrl.isNullOrBlank()) {
                NuvioAsyncImage(
                    model = artworkUrl,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    previewCache = true,
                )
            }

            if (trailerEnabled) {
                HomePosterHoverTrailer(
                    playbackSource = trailerPlaybackSource,
                    soundEnabled = trailerSoundEnabled,
                    startPositionSeconds = trailerStartSeconds,
                    onReady = onTrailerReady,
                    onError = onTrailerError,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.58f to previewBackground.copy(alpha = 0.08f),
                                1f to previewBackground.copy(alpha = 0.94f),
                            ),
                        ),
                    ),
            )

            if (logoUrl != null) {
                NuvioAsyncImage(
                    model = logoUrl,
                    contentDescription = stringResource(Res.string.poster_logo_content_description, item.name),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = NuvioTokens.Space.s18, bottom = NuvioTokens.Space.s14)
                        .width(180.dp)
                        .height(52.dp),
                    alignment = Alignment.CenterStart,
                    contentScale = ContentScale.Fit,
                    clipToBounds = false,
                    onError = { logoLoadError = true },
                    previewCache = true,
                )
            } else {
                Text(
                    text = item.name,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = NuvioTokens.Space.s18, vertical = NuvioTokens.Space.s16),
                    style = MaterialTheme.typography.headlineLarge,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            NuvioPosterWatchedOverlay(
                isWatched = isWatched,
                padding = NuvioTokens.Space.s10,
            )
        }

        Column(
            modifier = Modifier.padding(
                start = NuvioTokens.Space.s18,
                top = NuvioTokens.Space.s14,
                end = NuvioTokens.Space.s18,
                bottom = NuvioTokens.Space.s18,
            ),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s10),
        ) {
            Text(
                text = item.previewMetadataLine(),
                style = MaterialTheme.typography.labelLarge,
                color = tokens.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            item.genres
                .take(3)
                .joinToString(" • ")
                .takeIf { it.isNotBlank() }
                ?.let { genres ->
                    Text(
                        text = genres,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

            item.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (onClick != null) {
                Spacer(modifier = Modifier.height(NuvioTokens.Space.s2))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s10),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(NuvioTokens.Space.s40),
                        color = tokens.colors.accent,
                        contentColor = tokens.colors.onAccent,
                        shape = tokens.shapes.button,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(onClick = onClick)
                                .padding(horizontal = NuvioTokens.Space.s16),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(Res.string.home_view_details),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                            )
                            Spacer(modifier = Modifier.width(NuvioTokens.Space.s8))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(NuvioTokens.Icon.sm),
                            )
                        }
                    }

                    DetailIconAction(
                        label = if (isWatched) {
                            stringResource(Res.string.hero_mark_unwatched)
                        } else {
                            stringResource(Res.string.hero_mark_watched)
                        },
                        icon = if (isWatched) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.CheckCircleOutline
                        },
                        active = isWatched,
                        progress = 1f,
                        onClick = onWatchedClick,
                        size = NuvioTokens.Space.s40,
                    )

                    DetailIconAction(
                        label = if (isSaved) {
                            stringResource(Res.string.hero_remove_from_library)
                        } else {
                            stringResource(Res.string.hero_add_to_library)
                        },
                        icon = if (isSaved) Icons.Default.Check else Icons.Default.Add,
                        active = isSaved,
                        progress = 1f,
                        onClick = onSaveClick,
                        size = NuvioTokens.Space.s40,
                    )
                }
            }
        }
    }
}

private fun MetaPreview.previewMetadataLine(): String =
    buildList {
        add(type.replaceFirstChar(Char::uppercase))
        releaseInfo
            ?.takeIf { it.isNotBlank() }
            ?.let(::formatReleaseDateForDisplay)
            ?.takeIf { it.isNotBlank() }
            ?.let(::add)
        imdbRating
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { add("IMDb $it") }
    }.joinToString(" • ")

private class HomePosterPreviewPositionProvider(
    private val edgeMarginPx: Int,
    private val topOverlapPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - edgeMarginPx)
            .coerceAtLeast(edgeMarginPx)
        val maxY = (windowSize.height - popupContentSize.height - edgeMarginPx)
            .coerceAtLeast(edgeMarginPx)
        val centeredX = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        val preferredY = anchorBounds.top - topOverlapPx

        return IntOffset(
            x = centeredX.coerceIn(edgeMarginPx, maxX),
            y = preferredY.coerceIn(edgeMarginPx, maxY),
        )
    }
}
