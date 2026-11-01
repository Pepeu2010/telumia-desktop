package com.nuvio.app.features.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.nuvio.app.core.ui.NuvioTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class HomeHeroShelfOverlapTest {
    @Test
    fun `first shelf overlaps hero and following row starts directly after it`() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            var overlapPx = 0f
            setContent {
                NuvioTheme {
                    overlapPx = with(LocalDensity.current) { 24.dp.toPx() }
                    Column {
                        Box(Modifier.fillMaxWidth().height(100.dp).testTag("hero"))
                        Box(Modifier.overlapHomeHeroRail(24.dp)) {
                            Box(Modifier.fillMaxWidth().height(100.dp).testTag("shelf"))
                        }
                        Box(Modifier.fillMaxWidth().height(20.dp).testTag("next-shelf"))
                    }
                }
            }

            val hero = onNodeWithTag("hero").fetchSemanticsNode().boundsInRoot
            val shelf = onNodeWithTag("shelf").fetchSemanticsNode().boundsInRoot
            val nextShelf = onNodeWithTag("next-shelf").fetchSemanticsNode().boundsInRoot

            assertEquals(hero.bottom - overlapPx, shelf.top, 1f)
            assertEquals(shelf.bottom, nextShelf.top, 1f)
        }
}
