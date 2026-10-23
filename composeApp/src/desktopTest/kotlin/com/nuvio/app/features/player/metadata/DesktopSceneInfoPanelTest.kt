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

class DesktopSceneInfoPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun displaysOnlyProvidedFactsAndDismissesThroughItsCloseControl() {
        val open = mutableStateOf(true)
        val entries = listOf(
            SceneInfoEntry("trusted:actor", TimedMetadataKind.ACTOR, "Atriz de teste", "Personagem"),
            SceneInfoEntry("trusted:music", TimedMetadataKind.MUSIC, "Canção de teste", "Artista de teste"),
        )
        compose.setContent {
            NuvioTheme {
                Box(Modifier.size(620.dp, 520.dp)) {
                    if (open.value) DesktopSceneInfoPanel(entries) { open.value = false }
                }
            }
        }

        compose.onNodeWithTag("scene-info-panel").assertExists()
        compose.onNodeWithText("Atriz de teste").assertExists()
        compose.onNodeWithText("Canção de teste").assertExists()
        compose.onNodeWithTag("scene-info-close").performClick()
        compose.onNodeWithTag("scene-info-panel").assertDoesNotExist()
    }
}
