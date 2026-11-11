package com.nuvio.app.features.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.nuvio.app.core.ui.NavigationMotion
import com.nuvio.app.core.ui.NuvioTheme
import com.nuvio.app.features.home.components.HomePosterCard
import com.nuvio.app.features.home.components.HomeContinueWatchingSection
import com.nuvio.app.features.tracking.WatchProgressSource
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import com.nuvio.app.features.watchprogress.ContinueWatchingSectionStyle
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class HomePosterHighlightTest {
    @Test
    fun movingBetweenPostersUpdatesTheSelectedTitle() = runDesktopComposeUiTest(width = 900, height = 600) {
        val first = MetaPreview(id = "first", type = "movie", name = "Primeiro filme")
        val second = MetaPreview(id = "second", type = "movie", name = "Segundo filme")
        var selected by mutableStateOf<MetaPreview?>(null)
        setContent {
            NuvioTheme(navigationMotion = NavigationMotion.OFF) {
                Column {
                    Text("Destaque: ${selected?.name.orEmpty()}")
                    Row {
                        HomePosterCard(first, modifier = Modifier.testTag("first-poster"),
                            onClick = {}, onHighlighted = { selected = it })
                        HomePosterCard(second, modifier = Modifier.testTag("second-poster"),
                            onClick = {}, onHighlighted = { selected = it })
                    }
                }
            }
        }
        onNodeWithTag("first-poster").performMouseInput { enter(center) }
        onNodeWithText("Destaque: Primeiro filme").assertIsDisplayed()
        onNodeWithTag("first-poster").performMouseInput { exit() }
        onNodeWithTag("second-poster").performMouseInput { enter(center) }
        onNodeWithText("Destaque: Segundo filme").assertIsDisplayed()
    }

    @Test
    fun continueWatchingHighlightsByPointerAndKeyboardEvenWithoutArtwork() =
        runDesktopComposeUiTest(width = 900, height = 600) {
            val first = ContinueWatchingItem(parentMetaId = "first", parentMetaType = "movie", videoId = "first",
                title = "Primeiro em andamento", subtitle = "", imageUrl = null,
                resumePositionMs = 30_000, durationMs = 120_000, progressFraction = 0.25f)
            val second = first.copy(parentMetaId = "second", videoId = "second", title = "Segundo em andamento")
            var selected by mutableStateOf<ContinueWatchingItem?>(null)
            setContent {
                NuvioTheme(navigationMotion = NavigationMotion.OFF) {
                    Column {
                        Text("Destaque: ${selected?.title.orEmpty()}")
                        HomeContinueWatchingSection(items = listOf(first, second),
                            style = ContinueWatchingSectionStyle.Poster,
                            dataSourceKey = WatchProgressSource.NUVIO_SYNC,
                            cinematicPosterRail = true, onItemClick = {}, onItemHighlighted = { selected = it })
                    }
                }
            }
            onNodeWithContentDescription("Segundo em andamento").performMouseInput { enter(center) }
            onNodeWithText("Destaque: Segundo em andamento").assertIsDisplayed()
            onNodeWithContentDescription("Segundo em andamento").performMouseInput { exit() }
            onNodeWithContentDescription("Primeiro em andamento").performSemanticsAction(SemanticsActions.RequestFocus)
            onNodeWithText("Destaque: Primeiro em andamento").assertIsDisplayed()
        }
}
