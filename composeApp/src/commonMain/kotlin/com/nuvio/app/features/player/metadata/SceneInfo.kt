package com.nuvio.app.features.player.metadata

data class SceneInfoLoadResult(
    val scope: TimedMetadataScope,
    val positionMs: Long,
    val entries: List<SceneInfoEntry>,
    val failures: List<TimedMetadataProviderFailure>,
)

/** UI-safe scene facts projected only from trusted, edition-specific timed events. */
data class SceneInfoEntry(
    val id: String,
    val kind: TimedMetadataKind,
    val title: String,
    val detail: String? = null,
    val startMs: Long? = null,
    val chapterRelation: SceneInfoChapterRelation? = null,
)

enum class SceneInfoChapterRelation { PREVIOUS, CURRENT, NEXT }

data class SceneInfoChapterContext(
    val previous: SceneInfoEntry? = null,
    val current: SceneInfoEntry? = null,
    val next: SceneInfoEntry? = null,
) {
    fun asEntries(): List<SceneInfoEntry> = listOfNotNull(
        previous?.copy(chapterRelation = SceneInfoChapterRelation.PREVIOUS),
        current?.copy(chapterRelation = SceneInfoChapterRelation.CURRENT),
        next?.copy(chapterRelation = SceneInfoChapterRelation.NEXT),
    )
}

/**
 * Projects facts active at [positionMs]. The timeline enforces edition identity, provider trust,
 * temporal bounds, and explicit confidence before any fact reaches this presentation model.
 */
fun TimedMetadataTimeline.sceneInfoAt(
    positionMs: Long,
    minimumConfidence: Double = 0.8,
): List<SceneInfoEntry> = sceneEventsAt(positionMs, minimumConfidence).mapNotNull { event ->
    when (val body = event.body) {
        is TimedMetadataBody.Actor -> SceneInfoEntry(
            id = "${event.provenance.providerId}:${event.id}",
            kind = body.kind,
            title = body.name,
            detail = body.character,
        )
        is TimedMetadataBody.Music -> SceneInfoEntry(
            id = "${event.provenance.providerId}:${event.id}",
            kind = body.kind,
            title = body.title,
            detail = body.artist,
        )
        is TimedMetadataBody.Trivia -> SceneInfoEntry(
            id = "${event.provenance.providerId}:${event.id}",
            kind = body.kind,
            title = body.text,
        )
        is TimedMetadataBody.Chapter -> SceneInfoEntry(
            id = "${event.provenance.providerId}:${event.id}",
            kind = body.kind,
            title = body.title,
            startMs = event.startMs,
        )
        is TimedMetadataBody.Segment, is TimedMetadataBody.Bookmark -> null
    }
}

/** Projects the chapters around the playback instant without manufacturing chapter titles or boundaries. */
fun TimedMetadataTimeline.sceneInfoChapterContextAt(
    positionMs: Long,
    minimumConfidence: Double = 0.8,
): SceneInfoChapterContext {
    if (positionMs < 0) return SceneInfoChapterContext()
    val chapters = trustedSceneEvents(minimumConfidence).mapNotNull { event ->
        val body = event.body as? TimedMetadataBody.Chapter ?: return@mapNotNull null
        SceneInfoEntry(
            id = "${event.provenance.providerId}:${event.id}",
            kind = body.kind,
            title = body.title,
            startMs = event.startMs,
        ) to event
    }
    if (chapters.isEmpty()) return SceneInfoChapterContext()

    val currentIndex = chapters.indexOfLast { (_, event) ->
        event.startMs <= positionMs && (event.endMs == null || positionMs < event.endMs)
    }
    val previousIndex: Int
    val nextIndex: Int
    val current: SceneInfoEntry?
    if (currentIndex >= 0) {
        previousIndex = currentIndex - 1
        nextIndex = currentIndex + 1
        current = chapters[currentIndex].first
    } else {
        previousIndex = chapters.indexOfLast { (_, event) -> event.startMs <= positionMs }
        nextIndex = chapters.indexOfFirst { (_, event) -> event.startMs > positionMs }
        current = null
    }
    return SceneInfoChapterContext(
        previous = chapters.getOrNull(previousIndex)?.first,
        current = current,
        next = chapters.getOrNull(nextIndex)?.first,
    )
}

/** Loads scene facts through the registered-provider policy before projecting a playback instant. */
suspend fun loadSceneInfoAt(
    scope: TimedMetadataScope,
    positionMs: Long,
    providers: List<TimedMetadataProvider>,
    trustedSceneProviderIds: Set<String>,
    durationMs: Long? = null,
    minimumConfidence: Double = 0.8,
): SceneInfoLoadResult {
    if (scope.editionId.isNullOrBlank() || positionMs < 0) {
        return SceneInfoLoadResult(scope, positionMs, emptyList(), emptyList())
    }
    val loaded = loadTimedMetadata(
        scope = scope,
        providers = providers,
        durationMs = durationMs,
        trustedSceneProviderIds = trustedSceneProviderIds,
    )
    return SceneInfoLoadResult(
        scope = scope,
        positionMs = positionMs,
        entries = loaded.timeline.sceneInfoAt(positionMs, minimumConfidence),
        failures = loaded.failures,
    )
}
