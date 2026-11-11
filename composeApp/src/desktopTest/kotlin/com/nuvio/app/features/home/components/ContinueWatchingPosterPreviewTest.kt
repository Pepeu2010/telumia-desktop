package com.nuvio.app.features.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NavigationMotion
import com.nuvio.app.core.ui.NuvioTheme
import com.nuvio.app.core.ui.PosterCardStyleUiState
import com.nuvio.app.core.ui.PosterZoomOverlayCoordinator
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.awaitCancellation
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ContinueWatchingPosterPreviewTest {
    @Test
    fun hoverResolvesTheParentTitleAndPreviewRetainsResumeAction() =
        runDesktopComposeUiTest(width = 1366, height = 768) {
            val item = ContinueWatchingItem(parentMetaId = "series:123", parentMetaType = "series",
                videoId = "episode:2", title = "Minha série", subtitle = "Episódio 2", imageUrl = null,
                poster = "file:///fixture/poster.jpg", background = "file:///fixture/backdrop.jpg",
                resumePositionMs = 30_000, durationMs = 120_000, progressFraction = 0.25f)
            val resolved = AtomicReference<MetaPreview?>(null)
            val resumes = AtomicInteger()
            runOnIdle { PosterZoomOverlayCoordinator.hide() }
            setContent {
                NuvioTheme(navigationMotion = NavigationMotion.OFF) {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.padding(64.dp)) {
                            ContinueWatchingPosterPreview(item = item, enabled = true,
                                onClick = { resumes.incrementAndGet() }, onLongClick = null,
                                previewSettings = PosterCardStyleUiState(hoverPreviewEnabled = true,
                                    hoverPreviewOpenDelayMillis = 500, hoverPreviewTrailerEnabled = true),
                                trailerPreviewSupported = true,
                                trailerResolver = { preview -> resolved.set(preview); awaitCancellation() },
                            ) { hover ->
                                Box(hover.testTag("continue-anchor").size(200.dp, 300.dp).background(Color.DarkGray))
                            }
                        }
                        HomePosterHoverPreviewOverlayHost()
                    }
                }
            }
            onNodeWithTag("continue-anchor").performMouseInput { enter(center) }
            waitUntil(timeoutMillis = 4_000) { onAllNodesWithTag("home-hover-preview").fetchSemanticsNodes().isNotEmpty() }
            onNodeWithTag("home-hover-preview").performMouseInput { moveTo(center) }
            waitUntil(timeoutMillis = 8_000) { resolved.get() != null }
            assertEquals("series:123", resolved.get()?.id)
            assertEquals("series", resolved.get()?.type)
            assertEquals("file:///fixture/backdrop.jpg", resolved.get()?.banner)
            onNodeWithTag("home-hover-preview").performClick()
            assertEquals(1, resumes.get())
        }
}
