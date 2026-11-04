package com.nuvio.app.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import kotlin.test.Test
import org.junit.Rule

class NuvioAccentMarkerTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun solidCustomThemeRendersAccentMarker() {
        compose.setContent {
            NuvioTheme(appTheme = AppTheme.CUSTOM, customThemeColors = CustomThemeColors.solid(0x4488CC)) {
                Box { NuvioAccentMarker(modifier = Modifier.testTag("accent")) }
            }
        }
        compose.onNodeWithTag("accent").assertExists()
        compose.waitForIdle()
    }
}
