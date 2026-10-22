package com.nuvio.app.features.player.metadata

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class SceneInfoProjectionTest {
    private val scope = TimedMetadataScope("series:1", "series", "episode:1", "cut:1")
    private val trusted = setOf("licensed.scene-data")

    @Test
    fun projectsOnlyTrustedFactsActiveAtTheRequestedPosition() {
        val events = listOf(
            event("actor", 100, 300, TimedMetadataBody.Actor("person:1", "Pessoa", "Personagem")),
            event("music", 150, 250, TimedMetadataBody.Music("Canção", "Artista")),
            event("trivia", 200, 400, TimedMetadataBody.Trivia("Curiosidade")),
            event("chapter", 200, 500, TimedMetadataBody.Chapter("A chegada")),
            event("intro", 0, 120, TimedMetadataBody.Segment(TimedMetadataKind.INTRO)),
            event("bookmark", 200, null, TimedMetadataBody.Bookmark("Minha cena")),
        )
        val timeline = TimedMetadataTimeline.create(scope, events, trustedSceneProviderIds = trusted)

        assertEquals(
            listOf("actor", "music", "chapter", "trivia"),
            timeline.sceneInfoAt(220).map { it.id.substringAfter(':') },
        )
        assertEquals("Personagem", timeline.sceneInfoAt(220).first().detail)
        assertTrue(timeline.sceneInfoAt(300).none { it.title == "Canção" })
        assertTrue(timeline.sceneInfoAt(99).isEmpty())
    }

    @Test
    fun refusesFactsWithoutTrustedProviderEditionOrExplicitConfidence() {
        val actor = event("actor", 0, 100, TimedMetadataBody.Actor("person:1", "Pessoa"))
        assertTrue(TimedMetadataTimeline.create(scope, listOf(actor)).sceneInfoAt(50).isEmpty())
        assertEquals("Pessoa", TimedMetadataTimeline.create(scope, listOf(actor), trustedSceneProviderIds = trusted)
            .sceneInfoAt(50).single().title)
        assertTrue(TimedMetadataTimeline.create(scope.copy(editionId = null), listOf(actor.copy(confidence = 0.99)),
            trustedSceneProviderIds = trusted).sceneInfoAt(50).isEmpty())
        assertTrue(TimedMetadataTimeline.create(scope, listOf(actor.copy(confidence = 0.79)),
            trustedSceneProviderIds = trusted).sceneInfoAt(50).isEmpty())
        assertEquals("Pessoa", TimedMetadataTimeline.create(scope, listOf(actor.copy(confidence = 0.95)),
            trustedSceneProviderIds = trusted).sceneInfoAt(50).single().title)
    }

    @Test
    fun loadUsesProviderRegistrationAndDoesNotRequestMetadataWithoutAnEdition() = runBlocking {
        var trustedCalls = 0
        var genericCalls = 0
        val actor = event("actor", 0, 100, TimedMetadataBody.Actor("person:1", "Pessoa"))
            .copy(provenance = TimedMetadataProvenance("spoofed"), confidence = 0.95)
        val trustedProvider = object : SceneMetadataProvider {
            override val id = "licensed.scene-data"
            override suspend fun load(scope: TimedMetadataScope): List<TimedMetadataEvent> {
                trustedCalls++
                return listOf(actor)
            }
        }
        val genericProvider = object : TimedMetadataProvider {
            override val id = "generic.catalog"
            override suspend fun load(scope: TimedMetadataScope): List<TimedMetadataEvent> {
                genericCalls++
                return listOf(actor.copy(id = "generic-actor"))
            }
        }

        val result = loadSceneInfoAt(scope, 50, listOf(trustedProvider, genericProvider),
            trustedSceneProviderIds = setOf(trustedProvider.id, genericProvider.id), durationMs = 100)
        assertEquals(listOf("Pessoa"), result.entries.map { it.title })
        assertEquals("licensed.scene-data:actor", result.entries.single().id)
        assertEquals(1, trustedCalls)
        assertEquals(1, genericCalls)

        val noEdition = loadSceneInfoAt(scope.copy(editionId = null), 50, listOf(trustedProvider),
            trustedSceneProviderIds = setOf(trustedProvider.id), durationMs = 100)
        assertTrue(noEdition.entries.isEmpty())
        assertEquals(1, trustedCalls)
    }

    private fun event(id: String, start: Long, end: Long?, body: TimedMetadataBody) = TimedMetadataEvent(
        id = id,
        startMs = start,
        endMs = end,
        body = body,
        provenance = TimedMetadataProvenance("licensed.scene-data", "evidence:$id"),
        confidence = 0.95,
    )
}
