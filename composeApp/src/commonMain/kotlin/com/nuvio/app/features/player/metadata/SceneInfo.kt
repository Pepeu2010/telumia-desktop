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
)

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
        )
        is TimedMetadataBody.Segment, is TimedMetadataBody.Bookmark -> null
    }
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
