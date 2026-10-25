package com.nuvio.app.features.player.metadata

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class PlayerSceneInfoDesktopTest {
    private val media = TimedMetadataScope("movie:7", "movie", "video:7")

    @Test
    fun sceneInfoScopeRequiresResolvedMediaAndAPlayableSourceAndHashesTheSource() {
        assertNull(desktopSceneInfoScope(null, "https://media.example/film.mkv", sourceAvailable = true))
        assertNull(desktopSceneInfoScope(media, "https://media.example/film.mkv", sourceAvailable = false))
        assertNull(desktopSceneInfoScope(media.copy(mediaId = ""), "https://media.example/film.mkv", true))
        assertNull(desktopSceneInfoScope(media.copy(mediaType = "series", videoId = "movie:7"), "https://media.example/film.mkv", true))

        val resolved = assertNotNull(desktopSceneInfoScope(media, "https://media.example/film.mkv?token=private", true))
        assertEquals(media.mediaId, resolved.mediaId)
        assertEquals(media.videoId, resolved.videoId)
        assertTrue(resolved.editionId.orEmpty().matches(Regex("[a-f0-9]{64}")))
        assertFalse(resolved.editionId.orEmpty().contains("private"))
    }

    @Test
    fun chapterFactsAreProjectedOnlyForTheCurrentEditionAndTime() = runBlocking {
        val scope = assertNotNull(desktopSceneInfoScope(media, "https://media.example/film.mkv", true))
        val provider = EmbeddedChapterSceneMetadataProvider(
            scope,
            EmbeddedChapterSnapshot(listOf(EmbeddedChapter(0, 0, "A viagem"), EmbeddedChapter(1, 2_000, "O retorno")), complete = true),
            durationMs = 4_000,
        )
        val timeline = loadTimedMetadata(scope, listOf(provider), 4_000, setOf(provider.id)).timeline

        assertEquals(listOf("A viagem"), desktopSceneInfoEntriesAt(timeline, scope, 1_999).map(SceneInfoEntry::title))
        assertEquals(listOf("O retorno"), desktopSceneInfoEntriesAt(timeline, scope, 2_000).map(SceneInfoEntry::title))
        assertTrue(desktopSceneInfoEntriesAt(timeline, scope.copy(editionId = "0".repeat(64)), 2_000).isEmpty())
        assertTrue(desktopSceneInfoEntriesAt(timeline, scope, 4_000).isEmpty())
    }
}
