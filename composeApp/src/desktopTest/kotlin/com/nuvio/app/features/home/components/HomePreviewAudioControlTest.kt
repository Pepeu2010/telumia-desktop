package com.nuvio.app.features.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.nuvio.app.core.ui.NavigationMotion
import com.nuvio.app.core.ui.NuvioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomePreviewAudioControlTest {
    @get:Rule val compose = createComposeRule()

    @Test fun soundOptInDoesNotActivateTheUnderlyingMovieAndControlLeavesWithPlayback() {
        val ready = mutableStateOf(false)
        val sound = mutableStateOf(false)
        var movieActivations = 0
        compose.setContent {
            NuvioTheme(navigationMotion = NavigationMotion.OFF) {
                Box(Modifier.clickable { movieActivations++ }) {
                    HomePreviewAudioControl(visible = ready.value, soundEnabled = sound.value,
                        onToggleSound = { sound.value = !sound.value })
                }
            }
        }
        compose.onNodeWithTag("home-preview-audio").assertDoesNotExist()
        compose.runOnIdle { ready.value = true }
        compose.onNodeWithTag("home-preview-audio").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(true, sound.value)
            assertEquals(0, movieActivations)
        }
        compose.onNodeWithTag("home-preview-audio").performClick()
        compose.runOnIdle { assertEquals(false, sound.value); ready.value = false }
        compose.onNodeWithTag("home-preview-audio").assertDoesNotExist()
    }
}
