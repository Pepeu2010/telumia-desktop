package com.nuvio.app.features.player.metadata

/**
 * Exposes only chapter labels read from the currently opened media as Scene Info.
 * It is edition-scoped and never promotes chapter text into actor, music, or trivia claims.
 */
internal class EmbeddedChapterSceneMetadataProvider(
    private val editionScope: TimedMetadataScope,
    private val snapshot: EmbeddedChapterSnapshot,
    private val durationMs: Long,
) : SceneMetadataProvider {
    override val id: String = PROVIDER_ID

    override suspend fun load(scope: TimedMetadataScope): List<TimedMetadataEvent> {
        if (scope != editionScope || scope.editionId.isNullOrBlank()) return emptyList()
        return embeddedChapterTimedMetadata(scope, snapshot, durationMs).events.map { event ->
            event.copy(
                provenance = TimedMetadataProvenance(id, event.id),
                confidence = EMBEDDED_CHAPTER_CONFIDENCE,
            )
        }
    }

    companion object {
        const val PROVIDER_ID = "embedded-mpv-chapters"
        const val EMBEDDED_CHAPTER_CONFIDENCE = 1.0
    }
}
