package com.nuvio.app.features.details

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NavigationMotion
import com.nuvio.app.core.ui.NuvioTheme
import com.nuvio.app.features.details.components.DetailActionButtons
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class DetailActionButtonsTest {
    @get:Rule
    val compose = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun playActionCanBeFocusedAndActivatedWithKeyboard() {
        val playCount = mutableIntStateOf(0)
        compose.setContent {
            NuvioTheme(navigationMotion = NavigationMotion.OFF) {
                DetailActionButtons(
                    modifier = Modifier.width(420.dp),
                    playLabel = "Assistir",
                    onPlayClick = { playCount.intValue++ },
                )
            }
        }

        compose.onNodeWithText("Assistir")
            .assertIsDisplayed()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) { it() }
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        compose.runOnIdle { assertEquals(1, playCount.intValue) }
    }
}
