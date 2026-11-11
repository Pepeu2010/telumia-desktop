package com.nuvio.app.features.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import com.nuvio.app.core.ui.NavigationMotion
import com.nuvio.app.core.ui.NuvioTheme
import com.nuvio.app.features.home.components.HomeHeroSection
import com.nuvio.app.features.home.components.HomeSelectedHero
import com.sun.net.httpserver.HttpServer
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class HomeBackdropCoverageTest {
    @Test
    fun desktopBackdropContinuesBehindTheFirstShelf() {
        val artwork = BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB).apply {
            createGraphics().apply { color = Color(220, 35, 25); fillRect(0, 0, 640, 360); dispose() }
        }
        val bytes = ByteArrayOutputStream().also { ImageIO.write(artwork, "png", it) }.toByteArray()
        val requested = CountDownLatch(1)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/backdrop.png") { exchange ->
                exchange.responseHeaders.add("Content-Type", "image/png")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
                requested.countDown()
            }
            start()
        }
        try {
            runDesktopComposeUiTest(width = 1366, height = 768) {
                val movie = MetaPreview(id = "fixture:backdrop", type = "movie", name = "Cinema",
                    banner = "http://127.0.0.1:${server.address.port}/backdrop.png")
                setContent {
                    NuvioTheme(navigationMotion = NavigationMotion.OFF) {
                        LazyColumn(Modifier.fillMaxSize().testTag("backdrop-viewport")) {
                            item { HomeHeroSection(listOf(movie), viewportHeight = 768.dp) }
                            item {
                                Box(Modifier.offset(x = 50.dp, y = (-112).dp)
                                    .size(width = 160.dp, height = 230.dp)
                                    .background(androidx.compose.ui.graphics.Color.Blue))
                            }
                        }
                    }
                }
                waitUntil(timeoutMillis = 15_000) { requested.count == 0L }
                var redAtShelf = false
                waitUntil(timeoutMillis = 15_000) {
                    val image = onNodeWithTag("backdrop-viewport").captureToImage().toAwtImage()
                    val pixel = Color(image.getRGB(1100, 600))
                    redAtShelf = pixel.red > pixel.green + 35
                    if (redAtShelf) {
                        val shelf = Color(image.getRGB(100, 500))
                        assertTrue(shelf.blue > shelf.red + 35, "Shelf should render in front of the backdrop")
                        ImageIO.write(image, "png", File("build/reports/telumia-home/backdrop-coverage.png").apply { parentFile.mkdirs() })
                    }
                    redAtShelf
                }
                assertTrue(redAtShelf, "Backdrop should remain visible below the hero behind the first shelf")
            }
        } finally {
            server.stop(0)
            artwork.flush()
        }
    }

    @Test
    fun backdropRemainsVisibleBelowHeroDuringSelectionTransition() {
        fun png(color: Color): ByteArray {
            val image = BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB)
            image.createGraphics().apply { this.color = color; fillRect(0, 0, 640, 360); dispose() }
            return ByteArrayOutputStream().also { ImageIO.write(image, "png", it); image.flush() }.toByteArray()
        }
        val red = png(Color(220, 35, 25))
        val green = png(Color(25, 220, 35))
        val firstRequested = CountDownLatch(1)
        val secondRequested = CountDownLatch(1)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/red.png") { exchange ->
                exchange.responseHeaders.add("Content-Type", "image/png")
                exchange.sendResponseHeaders(200, red.size.toLong())
                exchange.responseBody.use { it.write(red) }
                firstRequested.countDown()
            }
            createContext("/green.png") { exchange ->
                exchange.responseHeaders.add("Content-Type", "image/png")
                exchange.sendResponseHeaders(200, green.size.toLong())
                exchange.responseBody.use { it.write(green) }
                secondRequested.countDown()
            }
            start()
        }
        try {
            runDesktopComposeUiTest(width = 1366, height = 768) {
                val first = MetaPreview("red", "movie", "Vermelho", banner = "http://127.0.0.1:${server.address.port}/red.png")
                val second = MetaPreview("green", "movie", "Verde", banner = "http://127.0.0.1:${server.address.port}/green.png")
                val selected = mutableStateOf<MetaPreview?>(null)
                setContent {
                    NuvioTheme(navigationMotion = NavigationMotion.FULL) {
                        LazyColumn(Modifier.fillMaxSize().testTag("transition-viewport")) {
                            item {
                                HomeSelectedHero(items = listOf(first), highlighted = selected.value,
                                    viewportHeight = 768.dp, mobileBelowSectionHeightHint = null,
                                    sectionPadding = null, listState = null, stretchPx = { 0f }, onItemClick = null)
                            }
                            item { Box(Modifier.offset(x = 50.dp, y = (-112).dp).size(160.dp, 230.dp)
                                .background(androidx.compose.ui.graphics.Color.Blue)) }
                        }
                    }
                }
                waitUntil(timeoutMillis = 15_000) { firstRequested.count == 0L }
                waitUntil(timeoutMillis = 15_000) {
                    val pixel = Color(onNodeWithTag("transition-viewport").captureToImage().toAwtImage().getRGB(1100, 600))
                    pixel.red > pixel.green + 35
                }
                mainClock.autoAdvance = false
                runOnIdle { selected.value = second }
                mainClock.advanceTimeByFrame()
                mainClock.advanceTimeByFrame()
                mainClock.advanceTimeByFrame()
                waitUntil(timeoutMillis = 15_000) { secondRequested.count == 0L }
                mainClock.advanceTimeBy(180)
                val image = onNodeWithTag("transition-viewport").captureToImage().toAwtImage()
                val pixel = Color(image.getRGB(1100, 600))
                val shelf = Color(image.getRGB(100, 500))
                val left = Color(image.getRGB(100, 100))
                val right = Color(image.getRGB(1100, 100))
                ImageIO.write(image, "png", File("build/reports/telumia-home/backdrop-transition.png").apply { parentFile.mkdirs() })
                assertTrue(pixel.red + pixel.green > 55, "Backdrop disappeared below the hero during the crossfade: $pixel")
                assertTrue(shelf.blue > shelf.red + 35 && shelf.blue > shelf.green + 35, "Shelf must remain in front: $shelf")
                assertTrue(left.red + left.green + left.blue + 35 < right.red + right.green + right.blue,
                    "Backdrop gradient must remain visible during the transition: left=$left right=$right")
            }
        } finally {
            server.stop(0)
        }
    }
}
