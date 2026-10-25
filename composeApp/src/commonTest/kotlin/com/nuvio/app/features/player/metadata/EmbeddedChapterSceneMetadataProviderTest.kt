package com.nuvio.app.features.player.metadata

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class EmbeddedChapterSceneMetadataProviderTest {
    private val scope = TimedMetadataScope("series:1", "series", "episode:2", "sha256:edition-a")
    private val provider = EmbeddedChapterSceneMetadataProvider(
        scope,
        EmbeddedChapterSnapshot(
            chapters = listOf(EmbeddedChapter(0, 0, "A chegada"), EmbeddedChapter(1, 500, "O encontro")),
            complete = true,
        ),
        durationMs = 1_000,
    )

    @Test
    fun embeddedChapterFactsAreScopedToTheExactEditionAndPlaybackInterval() = runBlocking {
        val loaded = loadTimedMetadata(
            scope = scope,
            providers = listOf(provider),
            durationMs = 1_000,
            trustedSceneProviderIds = setOf(provider.id),
        )

        assertEquals(listOf("A chegada"), loaded.timeline.sceneInfoAt(499).map(SceneInfoEntry::title))
        assertEquals(listOf("O encontro"), loaded.timeline.sceneInfoAt(500).map(SceneInfoEntry::title))
        assertTrue(loaded.timeline.sceneInfoAt(1_000).isEmpty())
        assertTrue(loaded.timeline.events.all { it.body.kind == TimedMetadataKind.CHAPTER })
        assertTrue(loaded.timeline.events.all { it.confidence == EmbeddedChapterSceneMetadataProvider.EMBEDDED_CHAPTER_CONFIDENCE })
    }

    @Test
    fun embeddedChaptersCannotBeReusedForAnotherEditionOrAnUnknownCut() = runBlocking {
        val differentEdition = scope.copy(editionId = "sha256:edition-b")
        val mismatched = loadTimedMetadata(
            scope = differentEdition,
            providers = listOf(provider),
            durationMs = 1_000,
            trustedSceneProviderIds = setOf(provider.id),
        )
        val unknownEdition = loadTimedMetadata(
            scope = scope.copy(editionId = null),
            providers = listOf(provider),
            durationMs = 1_000,
            trustedSceneProviderIds = setOf(provider.id),
        )

        assertTrue(mismatched.timeline.sceneInfoAt(100).isEmpty())
        assertTrue(unknownEdition.timeline.events.isEmpty())
    }
}
