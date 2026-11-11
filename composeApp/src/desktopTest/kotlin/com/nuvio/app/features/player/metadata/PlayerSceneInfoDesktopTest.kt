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
    fun chapterContextIsProjectedOnlyForTheCurrentEditionAndPlaybackTime() = runBlocking {
        val scope = assertNotNull(desktopSceneInfoScope(media, "https://media.example/film.mkv", true))
        val provider = EmbeddedChapterSceneMetadataProvider(
            scope,
            EmbeddedChapterSnapshot(
                listOf(
                    EmbeddedChapter(0, 0, "A viagem"),
                    EmbeddedChapter(1, 2_000, "O retorno"),
                    EmbeddedChapter(2, 3_500, "A despedida"),
                ),
                complete = true,
            ),
            durationMs = 4_000,
        )
        val timeline = loadTimedMetadata(scope, listOf(provider), 4_000, setOf(provider.id)).timeline

        val firstContext = desktopSceneInfoEntriesAt(timeline, scope, 1_999)
        assertEquals(listOf("A viagem", "O retorno"), firstContext.map(SceneInfoEntry::title))
        assertEquals(listOf(SceneInfoChapterRelation.CURRENT, SceneInfoChapterRelation.NEXT), firstContext.map(SceneInfoEntry::chapterRelation))
        assertEquals(listOf(0L, 2_000L), firstContext.map(SceneInfoEntry::startMs))

        val middleContext = desktopSceneInfoEntriesAt(timeline, scope, 2_000)
        assertEquals(listOf("A viagem", "O retorno", "A despedida"), middleContext.map(SceneInfoEntry::title))
        assertEquals(
            listOf(SceneInfoChapterRelation.PREVIOUS, SceneInfoChapterRelation.CURRENT, SceneInfoChapterRelation.NEXT),
            middleContext.map(SceneInfoEntry::chapterRelation),
        )
        assertEquals(listOf(0L, 2_000L, 3_500L), middleContext.map(SceneInfoEntry::startMs))
        assertTrue(desktopSceneInfoEntriesAt(timeline, scope.copy(editionId = "0".repeat(64)), 2_000).isEmpty())
        val endContext = desktopSceneInfoEntriesAt(timeline, scope, 4_000)
        assertEquals(listOf("A despedida"), endContext.map(SceneInfoEntry::title))
        assertEquals(listOf(SceneInfoChapterRelation.PREVIOUS), endContext.map(SceneInfoEntry::chapterRelation))
    }
}
