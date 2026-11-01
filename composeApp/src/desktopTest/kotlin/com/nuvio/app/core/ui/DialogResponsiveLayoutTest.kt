package com.nuvio.app.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class DialogResponsiveLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun narrowActionRowNeverPassesNegativeWidthsToItsChildren() {
        compose.setContent {
            NuvioTheme(navigationMotion = NavigationMotion.OFF) {
                Column(Modifier.width(1.dp)) {
                    DialogButtons {
                        Text("Salvar")
                        Text("Cancelar")
                        Text("Restaurar")
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Salvar").assertExists()
    }

    @Test fun longActionsRemainReachableInCompactWindow() {
        compose.setContent {
            NuvioTheme(navigationMotion = NavigationMotion.OFF) {
                Column(Modifier.width(200.dp)) {
                    DialogButtons {
                        DialogButton("Salvar alterações do perfil", {})
                        DialogButton("Cancelar e voltar", {})
                    }
                }
            }
        }
        compose.onNodeWithText("Salvar alterações do perfil").assertIsDisplayed()
        compose.onNodeWithText("Cancelar e voltar").assertIsDisplayed()
    }
}
