package com.nuvio.app.features.player.metadata

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioTheme
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopSceneInfoPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun displaysOnlyProvidedFactsAndDismissesThroughItsCloseControl() {
        val open = mutableStateOf(true)
        val seekPosition = mutableStateOf(-1L)
        val entries = listOf(
            SceneInfoEntry("trusted:actor", TimedMetadataKind.ACTOR, "Atriz de teste", "Personagem"),
            SceneInfoEntry("trusted:music", TimedMetadataKind.MUSIC, "Canção de teste", "Artista de teste"),
            SceneInfoEntry(
                "embedded-mpv-chapters:0:0", TimedMetadataKind.CHAPTER, "A chegada",
                startMs = 0L, chapterRelation = SceneInfoChapterRelation.PREVIOUS,
            ),
            SceneInfoEntry(
                "embedded-mpv-chapters:1:120000", TimedMetadataKind.CHAPTER, "O encontro",
                startMs = 120_000L, chapterRelation = SceneInfoChapterRelation.CURRENT,
            ),
            SceneInfoEntry(
                "embedded-mpv-chapters:2:240000", TimedMetadataKind.CHAPTER, "A despedida",
                startMs = 240_000L, chapterRelation = SceneInfoChapterRelation.NEXT,
            ),
        )
        compose.setContent {
            NuvioTheme {
                Box(Modifier.size(620.dp, 520.dp)) {
                    if (open.value) DesktopSceneInfoPanel(
                        entries = entries,
                        onDismiss = { open.value = false },
                        onSeekToChapterStart = { seekPosition.value = it },
                    )
                }
            }
        }

        compose.onNodeWithTag("scene-info-panel").assertExists()
        compose.onNodeWithText("Atriz de teste").assertExists()
        compose.onNodeWithText("Canção de teste").assertExists()
        compose.onNodeWithText("A chegada").assertExists()
        compose.onNodeWithText("O encontro").assertExists()
        compose.onNodeWithText("A despedida").assertExists()
        compose.onNodeWithTag("scene-info-chapter-seek-previous").performClick()
        compose.runOnIdle { assertEquals(0L, seekPosition.value) }
        compose.onNodeWithTag("scene-info-close").performClick()
        compose.onNodeWithTag("scene-info-panel").assertDoesNotExist()
    }
}
