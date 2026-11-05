package com.nuvio.app.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.*
import com.nuvio.app.features.catalog.CatalogTarget
import com.nuvio.app.features.home.components.*
import com.nuvio.app.features.tracking.WatchProgressSource
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import com.nuvio.app.features.watchprogress.ContinueWatchingSectionStyle
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

/** Production components with sample content. Optional artwork is local QA data, never app defaults. */
@OptIn(ExperimentalTestApi::class)
class CinematicHomeCompositionTest {
    @Test fun fullHdHomeKeepsTvDiscoverableAndDetailsConnectedToTheDisplayedMovie() =
        runDesktopComposeUiTest(width = 1920, height = 1080) {
            fun art(name: String) = File("build/qa-art/$name").takeIf(File::isFile)?.toURI()?.toString()
            val titles = listOf("Duna", "Interestelar", "A Origem", "Blade Runner 2049")
            val items = titles.mapIndexed { index, title -> MetaPreview("fixture:cinema:$index", "movie", title,
                poster = art("$index-poster.jpg"), banner = art("$index-background.jpg"),
                releaseInfo = "2021", imdbRating = "8.0", genres = listOf("Aventura", "Ficção científica"),
                description = "Em um futuro distante, uma jornada pelo deserto transforma o destino de uma família e de um planeta.") }
            val featured = items.first().copy(logo = art("logo.png"), banner = art("background.jpg"))
            val watching = items.mapIndexed { index, movie -> ContinueWatchingItem(
                parentMetaId = movie.id, parentMetaType = "movie", videoId = movie.id, title = movie.name,
                subtitle = "", imageUrl = movie.banner, background = movie.banner, poster = movie.poster,
                resumePositionMs = 2_400_000L, durationMs = 7_200_000L, progressFraction = 0.33f + index * 0.05f) }
            val destination = mutableStateOf("Início")
            var selectedMovie: MetaPreview? = null
            setContent { NuvioTheme(navigationMotion = NavigationMotion.OFF) {
                Box(Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                    .testTag("cinematic-home")) {
                    LazyColumn {
                        item { HomeHeroSection(listOf(featured), viewportHeight = 1080.dp,
                            previewTrailersSupported = false, onItemClick = { selectedMovie = it }) }
                        item { HomeContinueWatchingSection(watching, ContinueWatchingSectionStyle.Card,
                            WatchProgressSource.NUVIO_SYNC, title = "Continuar assistindo", sectionPadding = 64.dp,
                            modifier = Modifier.overlapHomeHeroRail(110.dp), onItemClick = {}) }
                        item { HomeCatalogRowSection(HomeCatalogSection("qa", "Para sua próxima sessão", "", "QA",
                            CatalogTarget.Library("movie", "all"), items), sectionPadding = 64.dp) }
                    }
                    DesktopNavigationBar(items = listOf(
                        "Início" to Icons.Default.Home, "Busca" to Icons.Default.Search,
                        "Biblioteca" to Icons.Default.VideoLibrary, "TV" to Icons.Default.LiveTv,
                        "Configurações" to Icons.Default.Settings,
                    ).map { (label, icon) -> FloatingNavigationItem(label, destination.value == label,
                        onClick = { destination.value = label }, icon = icon) },
                        modifier = Modifier.align(Alignment.TopCenter), isHeroEnabled = true)
                }
            } }
            onNodeWithText("Continuar assistindo").assertIsDisplayed()
            // Allow the optional local image decoder to finish; actions are asserted independently of art.
            waitForIdle()
            Thread.sleep(800)
            waitForIdle()
            val output = File("build/reports/telumia-cinematic/home-full-hd.png")
            output.parentFile.mkdirs()
            ImageIO.write(onNodeWithTag("cinematic-home").captureToImage().toAwtImage(), "png", output)
            onNodeWithTag("home-hero-details-action").assertIsDisplayed().performClick()
            runOnIdle { assertEquals(featured, selectedMovie) }
            onNodeWithContentDescription("TV").assertIsDisplayed().performClick()
            runOnIdle { assertEquals("TV", destination.value) }
        }
}
