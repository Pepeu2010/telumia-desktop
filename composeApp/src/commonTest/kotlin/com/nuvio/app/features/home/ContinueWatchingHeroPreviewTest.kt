package com.nuvio.app.features.home

import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import kotlin.test.Test
import kotlin.test.assertEquals

class ContinueWatchingHeroPreviewTest {
    private val progress = ContinueWatchingItem(
        parentMetaId = "movie-1", parentMetaType = "movie", videoId = "movie-1",
        title = "Filme em andamento", subtitle = "", imageUrl = "episode.jpg",
        background = "backdrop.jpg", poster = "poster.jpg", logo = "logo.png",
        resumePositionMs = 120_000, durationMs = 600_000, progressFraction = 0.2f,
    )

    @Test
    fun progressArtworkSurvivesCatalogRefreshWithBlankFields() {
        val refreshed = MetaPreview("movie-1", "movie", "Título do catálogo",
            banner = "", poster = " ", logo = null, imdbRating = "8.0")
        val hero = continueWatchingHeroPreview(progress, refreshed)
        assertEquals("backdrop.jpg", hero.banner)
        assertEquals("poster.jpg", hero.poster)
        assertEquals("logo.png", hero.logo)
        assertEquals("Título do catálogo", hero.name)
        assertEquals("8.0", hero.imdbRating)
    }

    @Test
    fun catalogArtworkWinsAndProgressWorksWithoutCatalog() {
        val catalog = MetaPreview("movie-1", "movie", "Catálogo", banner = "catalog.jpg")
        assertEquals("catalog.jpg", continueWatchingHeroPreview(progress, catalog).banner)
        val withoutCatalog = continueWatchingHeroPreview(progress, null)
        assertEquals("Filme em andamento", withoutCatalog.name)
        assertEquals("backdrop.jpg", withoutCatalog.banner)
        assertEquals("episode.jpg", continueWatchingHeroPreview(progress.copy(background = ""), null).banner)
    }

    @Test
    fun fallbackUsesUniqueProgressTitlesAndAvailableCatalogMetadata() {
        val series = progress.copy(parentMetaId = "series-1", parentMetaType = "series", videoId = "episode-1", title = "Série")
        val nextEpisode = series.copy(videoId = "episode-2")
        val catalog = MetaPreview("series-1", "series", "Nome do catálogo", banner = "series.jpg")
        val result = continueWatchingHeroFallback(listOf(progress.copy(isNextUp = true), series, nextEpisode,
            progress.copy(parentMetaId = "")), listOf(catalog))
        assertEquals(1, result.size)
        assertEquals("Nome do catálogo", result.single().name)
        assertEquals("series.jpg", result.single().banner)
    }
}
