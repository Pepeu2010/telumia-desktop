package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.nuvio.app.core.ui.NavigationMotion
import com.nuvio.app.core.ui.NuvioTheme
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_settings_page_appearance
import org.jetbrains.compose.resources.stringResource
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SettingsRootCompositionTest {
    @Test fun appearanceSpotlightFitsAndOpensOnSmallDesktop() = runDesktopComposeUiTest(width = 800, height = 600) {
        var title = ""
        var opens = 0
        setContent {
            NuvioTheme(navigationMotion = NavigationMotion.OFF) {
                title = stringResource(Res.string.compose_settings_page_appearance)
                LazyColumn(Modifier.fillMaxSize().testTag("settings-root")) {
                    settingsRootContent(isTablet = false, onPlaybackClick = {}, onAppearanceClick = { opens++ },
                        onAdvancedClick = {}, onNotificationsClick = {}, onContentDiscoveryClick = {},
                        onIntegrationsClick = {}, onTrackingClick = {}, onSupportersContributorsClick = {},
                        onLicensesAttributionsClick = {}, onAccountClick = {}, showAccountSection = false,
                        showAboutSection = false, showAdvancedSection = false, showAttribution = false)
                }
            }
        }
        val spotlight = onNodeWithText(title)
        spotlight.assertIsDisplayed()
        val bounds = spotlight.fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.left >= 0f && bounds.right <= 800f && bounds.bottom <= 600f)
        val image = onNodeWithTag("settings-root").captureToImage().toAwtImage()
        ImageIO.write(image, "png", File("build/reports/telumia-settings/root-800.png").apply { parentFile.mkdirs() })
        spotlight.performClick()
        runOnIdle { assertEquals(1, opens) }
    }
}
