package com.nuvio.app.features.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.DesktopNavigationBar
import com.nuvio.app.core.ui.FloatingNavigationItem
import com.nuvio.app.core.ui.NavigationMotion
import com.nuvio.app.core.ui.NuvioTheme
import com.nuvio.app.features.home.components.HomeContinueWatchingSection
import com.nuvio.app.features.home.components.HomeSelectedHero
import com.nuvio.app.features.tracking.WatchProgressSource
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import com.nuvio.app.features.watchprogress.ContinueWatchingSectionStyle
import com.sun.net.httpserver.HttpServer
import java.awt.Color
import java.awt.GradientPaint
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class HomeCinematicCompositionTest {
    @Test
    fun realNavigationHeroAndContinueWatchingRespondTogether() {
        val artwork = BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB).apply {
            createGraphics().apply {
                paint = GradientPaint(0f, 0f, Color(20, 35, 38), 1280f, 720f, Color(75, 150, 146))
                fillRect(0, 0, 1280, 720)
                dispose()
            }
        }
        val bytes = ByteArrayOutputStream().also { ImageIO.write(artwork, "png", it) }.toByteArray()
        val artworkRequested = CountDownLatch(1)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/art.png") { exchange ->
                exchange.responseHeaders.add("Content-Type", "image/png")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
                artworkRequested.countDown()
            }
            start()
        }
        try {
            val imageUrl = "http://127.0.0.1:${server.address.port}/art.png"
            val first = MetaPreview("one", "movie", "Horizonte", banner = imageUrl,
                releaseInfo = "2025", imdbRating = "8.0", genres = listOf("Aventura"),
                description = "Uma viagem por paisagens e histórias inesquecíveis.")
            val second = first.copy(id = "two", name = "Nova jornada")
            val progress = listOf(first, second).map { item ->
                ContinueWatchingItem(parentMetaId = item.id, parentMetaType = item.type,
                    videoId = item.id, title = item.name, subtitle = "", imageUrl = imageUrl,
                    resumePositionMs = 30_000, durationMs = 120_000, progressFraction = 0.25f)
            }
            runDesktopComposeUiTest(width = 1366, height = 768) {
                var highlighted by mutableStateOf<MetaPreview?>(null)
                setContent {
                    NuvioTheme(navigationMotion = NavigationMotion.OFF) {
                        Box(Modifier.fillMaxSize().testTag("cinematic-home")) {
                            LazyColumn(Modifier.fillMaxSize()) {
                                item {
                                    HomeSelectedHero(listOf(first), highlighted, 768.dp, null, null,
                                        null, { 0f }, {})
                                }
                                item {
                                    HomeContinueWatchingSection(items = progress,
                                        style = ContinueWatchingSectionStyle.Poster,
                                        dataSourceKey = WatchProgressSource.NUVIO_SYNC,
                                        title = "Continuar assistindo", cinematicPosterRail = true,
                                        onItemClick = {}, onItemHighlighted = { item ->
                                            highlighted = if (item.parentMetaId == second.id) second else first
                                        })
                                }
                            }
                            DesktopNavigationBar(items = listOf(
                                FloatingNavigationItem("Home", true, {}, Icons.Default.Home),
                                FloatingNavigationItem("Busca", false, {}, Icons.Default.Search),
                                FloatingNavigationItem("Configurações", false, {}, Icons.Default.Settings),
                            ))
                        }
                    }
                }
                onNodeWithText("Horizonte").assertIsDisplayed()
                onNodeWithText("Continuar assistindo").assertIsDisplayed()
                waitUntil(timeoutMillis = 15_000) { artworkRequested.count == 0L }
                waitUntil(timeoutMillis = 15_000) {
                    val pixel = Color(onNodeWithTag("cinematic-home").captureToImage().toAwtImage().getRGB(1100, 300))
                    pixel.green > pixel.red + 15
                }
                onNodeWithContentDescription("Nova jornada").performMouseInput { enter(center) }
                onNodeWithText("Nova jornada").assertIsDisplayed()
                waitUntil(timeoutMillis = 15_000) {
                    val pixel = Color(onNodeWithTag("cinematic-home").captureToImage().toAwtImage().getRGB(1100, 300))
                    pixel.green > pixel.red + 15
                }
                val posterBounds = onNode(hasContentDescription("Nova jornada") and hasClickAction())
                    .fetchSemanticsNode().boundsInRoot
                assertTrue(posterBounds.bottom <= 768f, "The first poster rail must fit inside the viewport")
                val image = onNodeWithTag("cinematic-home").captureToImage().toAwtImage()
                ImageIO.write(image, "png", File("build/reports/telumia-home/composition.png").apply { parentFile.mkdirs() })
            }
        } finally {
            server.stop(0)
            artwork.flush()
        }
    }
}
