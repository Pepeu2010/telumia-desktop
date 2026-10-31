package com.nuvio.app

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.LocalUiMotion
import com.nuvio.app.core.ui.NuvioAsyncImage
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.catalog.fetchCatalogPage
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.livetv.LiveTvCatalogResponse
import com.nuvio.app.features.livetv.LiveTvChannelItem
import com.nuvio.app.features.livetv.installedLiveTvCatalogs
import com.nuvio.app.features.livetv.projectLiveTvChannels
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.live_tv_addons_count
import nuvio.composeapp.generated.resources.live_tv_badge
import nuvio.composeapp.generated.resources.live_tv_channel_count
import nuvio.composeapp.generated.resources.live_tv_empty_body
import nuvio.composeapp.generated.resources.live_tv_empty_title
import nuvio.composeapp.generated.resources.live_tv_failed_body
import nuvio.composeapp.generated.resources.live_tv_loading
import nuvio.composeapp.generated.resources.live_tv_manage_addons
import nuvio.composeapp.generated.resources.live_tv_no_results
import nuvio.composeapp.generated.resources.live_tv_search
import nuvio.composeapp.generated.resources.live_tv_section_title
import nuvio.composeapp.generated.resources.live_tv_subtitle
import nuvio.composeapp.generated.resources.live_tv_title
import nuvio.composeapp.generated.resources.live_tv_watch
import nuvio.composeapp.generated.resources.compose_nav_tv
import nuvio.composeapp.generated.resources.live_tv_retry
import org.jetbrains.compose.resources.stringResource

private const val LIVE_TV_CATALOG_ITEM_LIMIT = 60
private const val LIVE_TV_PARALLEL_CATALOG_LIMIT = 4

private data class LiveTvFetchOutcome(
    val response: LiveTvCatalogResponse? = null,
    val failed: Boolean = false,
)

@Composable
internal fun LiveTvScreen(
    modifier: Modifier = Modifier,
    topChromePadding: Dp? = null,
    isSelectedTab: Boolean,
    onChannelClick: ((LiveTvChannelItem) -> Unit)?,
    onManageAddonsClick: () -> Unit,
) {
    val addonState by AddonRepository.uiState.collectAsStateWithLifecycle()
    var channels by remember { mutableStateOf(emptyList<LiveTvChannelItem>()) }
    var isLoading by remember { mutableStateOf(false) }
    var failedCatalogCount by remember { mutableIntStateOf(0) }
    var refreshVersion by remember { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val liveTvCatalogRequests = remember(addonState.addons) {
        installedLiveTvCatalogs(addonState.addons)
    }

    LaunchedEffect(isSelectedTab, refreshVersion, liveTvCatalogRequests) {
        if (!isSelectedTab) return@LaunchedEffect
        AddonRepository.initialize()
        isLoading = true
        failedCatalogCount = 0
        try {
            val readyState = AddonRepository.uiState.first { state ->
                state.addons.none { addon -> addon.enabled && addon.manifest == null && addon.isRefreshing }
            }
            val requests = installedLiveTvCatalogs(readyState.addons)
            val limiter = Semaphore(LIVE_TV_PARALLEL_CATALOG_LIMIT)
            val outcomes = coroutineScope {
                requests.map { request ->
                    async {
                        limiter.withPermit {
                            try {
                                LiveTvFetchOutcome(
                                    response = LiveTvCatalogResponse(
                                        request = request,
                                        items = fetchCatalogPage(
                                            manifestUrl = request.manifestUrl,
                                            type = request.catalog.type,
                                            catalogId = request.catalog.id,
                                            maxItems = LIVE_TV_CATALOG_ITEM_LIMIT,
                                            forceRefresh = refreshVersion > 0,
                                        ).items,
                                    ),
                                )
                            } catch (error: CancellationException) {
                                throw error
                            } catch (_: Exception) {
                                LiveTvFetchOutcome(failed = true)
                            }
                        }
                    }
                }.awaitAll()
            }
            channels = projectLiveTvChannels(outcomes.mapNotNull(LiveTvFetchOutcome::response))
            failedCatalogCount = outcomes.count(LiveTvFetchOutcome::failed)
        } finally {
            isLoading = false
        }
    }

    val normalizedQuery = searchQuery.trim()
    val visibleChannels = remember(channels, normalizedQuery) {
        if (normalizedQuery.isBlank()) channels else channels.filter { channel ->
            channel.meta.name.contains(normalizedQuery, ignoreCase = true) ||
                channel.sourceNames.any { it.contains(normalizedQuery, ignoreCase = true) }
        }
    }
    val sourceCount = remember(channels) { channels.flatMap { it.sourceNames }.distinct().size }
    val topPadding = maxOf(topChromePadding ?: 0.dp, 24.dp)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 190.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 36.dp, end = 36.dp, top = topPadding, bottom = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                LiveTvHero(
                    channelCount = channels.size,
                    sourceCount = sourceCount,
                    onRefresh = { refreshVersion++ },
                )
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth < 720.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(Res.string.live_tv_section_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                LiveTvChannelCountBadge(channels.size)
                            }
                            LiveTvSearchField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LiveTvSearchField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1.15f).heightIn(min = 56.dp),
                            )
                            Text(
                                text = stringResource(Res.string.live_tv_section_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            LiveTvChannelCountBadge(channels.size)
                        }
                    }
                }
            }
        }

        when {
            isLoading -> item(span = { GridItemSpan(maxLineSpan) }) {
                LiveTvEmptyPanel(
                    title = stringResource(Res.string.live_tv_loading),
                    body = stringResource(Res.string.live_tv_subtitle),
                    loading = true,
                )
            }

            visibleChannels.isEmpty() && channels.isNotEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                LiveTvEmptyPanel(
                    title = stringResource(Res.string.live_tv_no_results),
                    body = stringResource(Res.string.live_tv_search),
                )
            }

            visibleChannels.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                LiveTvEmptyPanel(
                    title = stringResource(Res.string.live_tv_empty_title),
                    body = stringResource(
                        if (failedCatalogCount > 0) Res.string.live_tv_failed_body else Res.string.live_tv_empty_body,
                    ),
                    onManageAddonsClick = onManageAddonsClick,
                    onRetryClick = { refreshVersion++ },
                    failed = failedCatalogCount > 0,
                )
            }

            else -> items(visibleChannels, key = LiveTvChannelItem::key) { channel ->
                LiveTvChannelCard(
                    channel = channel,
                    onClick = { onChannelClick?.invoke(channel) },
                )
            }
        }
    }
}

@Composable
private fun LiveTvSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        placeholder = { Text(stringResource(Res.string.live_tv_search)) },
        shape = RoundedCornerShape(18.dp),
    )
}

@Composable
private fun LiveTvChannelCountBadge(channelCount: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        shape = CircleShape,
    ) {
        Text(
            text = stringResource(Res.string.live_tv_channel_count, channelCount),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LiveTvHero(channelCount: Int, sourceCount: Int, onRefresh: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(208.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF151820), Color(0xFF262032), Color(0xFF462723)),
                ),
            ),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .size(280.dp)
                .alpha(0.22f)
                .background(Brush.radialGradient(listOf(colors.primary, Color.Transparent)), CircleShape),
        )
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 32.dp, end = 26.dp, top = 26.dp, bottom = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(8.dp).background(Color(0xFFFF6B56), CircleShape))
                    Text(
                        text = stringResource(Res.string.compose_nav_tv).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFFB281),
                    )
                }
                Text(
                    text = stringResource(Res.string.live_tv_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(Res.string.live_tv_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(Res.string.live_tv_addons_count, sourceCount, channelCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.84f),
                )
            }
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Tv,
                    contentDescription = null,
                    modifier = Modifier.size(124.dp).alpha(0.18f),
                    tint = Color.White,
                )
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(Res.string.live_tv_retry),
                        modifier = Modifier.size(28.dp),
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveTvChannelCard(channel: LiveTvChannelItem, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val motion = LocalUiMotion.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    val emphasized = hovered || focused
    val scale by animateFloatAsState(
        targetValue = if (emphasized && motion.allowsSpatialEffects) 1.035f else 1f,
        animationSpec = tween(motion.durationMillis(180), easing = FastOutSlowInEasing),
        label = "live_tv_channel_hover_scale",
    )
    val shape = RoundedCornerShape(22.dp)
    val artwork = channel.meta.banner ?: channel.meta.poster
    val centeredLogo = channel.meta.logo ?: channel.meta.poster.takeIf { artwork == null }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(218.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .hoverable(interaction)
            .onFocusChanged { focused = it.isFocused }
            .animateContentSize(),
        shape = shape,
        color = colors.surfaceVariant,
        border = BorderStroke(
            width = if (emphasized) 1.5.dp else 1.dp,
            color = if (emphasized) colors.primary.copy(alpha = 0.8f) else colors.outline.copy(alpha = 0.3f),
        ),
        shadowElevation = if (emphasized) 15.dp else 4.dp,
    ) {
        Box(Modifier.fillMaxSize()) {
            if (!artwork.isNullOrBlank()) {
                NuvioAsyncImage(
                    model = artwork,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.72f), colors.tertiary.copy(alpha = 0.42f), colors.surfaceVariant)),
                    ),
                )
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.12f), Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.92f))),
                ),
            )
            Row(
                Modifier.align(Alignment.TopStart).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Surface(color = Color(0xFFE8473C), shape = RoundedCornerShape(7.dp)) {
                    Text(
                        text = stringResource(Res.string.live_tv_badge),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }
            if (!centeredLogo.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.align(Alignment.Center).size(74.dp),
                    color = Color.Black.copy(alpha = 0.28f),
                    shape = CircleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        NuvioAsyncImage(
                            model = centeredLogo,
                            contentDescription = channel.meta.name,
                            modifier = Modifier.fillMaxSize().padding(14.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.LiveTv,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center).size(52.dp).alpha(0.86f),
                    tint = Color.White,
                )
            }
            Column(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = channel.meta.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = channel.sourceNames.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (emphasized) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    color = colors.primary,
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(Res.string.live_tv_watch),
                        modifier = Modifier.padding(7.dp).size(20.dp),
                        tint = colors.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveTvEmptyPanel(
    title: String,
    body: String,
    loading: Boolean = false,
    failed: Boolean = false,
    onManageAddonsClick: (() -> Unit)? = null,
    onRetryClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceVariant.copy(alpha = 0.44f),
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(30.dp), strokeWidth = 3.dp)
            } else {
                Icon(Icons.Filled.LiveTv, contentDescription = null, modifier = Modifier.size(34.dp), tint = colors.primary)
            }
            Text(title, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (!loading && onManageAddonsClick != null) {
                Button(onClick = onManageAddonsClick) {
                    Text(stringResource(Res.string.live_tv_manage_addons))
                }
            }
            if (!loading && failed && onRetryClick != null) {
                Button(onClick = onRetryClick) {
                    Text(stringResource(Res.string.live_tv_retry))
                }
            }
        }
    }
}
