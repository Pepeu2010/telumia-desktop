package com.nuvio.app.features.livetv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DesktopLiveTvScheduleTest {
    @Test fun guideShowsActualProgrammeDetailsMovesInTimeAndCanPlayTheChannel() =
        runDesktopComposeUiTest(width = 960, height = 600) {
            val start = 1_800_000_000_000L
            val channel = LiveTvChannel("news", "source", "Notícias", "https://example.com/live", tvgId = "news.br")
            val current = LiveTvProgram("source", "news.br", "Jornal", start, start + 1_800_000,
                description = "Resumo das notícias")
            val later = LiveTvProgram("source", "news.br", "Entrevista", start + 7_200_000, start + 9_000_000)
            var played: LiveTvChannel? = null
            setContent {
                MaterialTheme {
                    Box(Modifier.fillMaxSize()) {
                        DesktopLiveTvSchedule(listOf(channel), listOf(current, later), start + 900_000) { played = it }
                    }
                }
            }
            onNodeWithText("Agora · Jornal").assertIsDisplayed().performClick()
            onNodeWithText("Resumo das notícias").assertIsDisplayed()
            onNodeWithText("Próximo").performClick()
            onNodeWithText("Entrevista").assertIsDisplayed()
            onNodeWithText("Resumo das notícias").assertDoesNotExist()
            onNodeWithText("Agora").performClick()
            onNodeWithText("Agora · Jornal").assertIsDisplayed()
            onNodeWithText("Notícias").performClick()
            runOnIdle { assertEquals(channel, played) }
        }
}
