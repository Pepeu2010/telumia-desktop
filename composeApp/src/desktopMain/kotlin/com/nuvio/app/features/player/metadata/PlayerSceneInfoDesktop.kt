package com.nuvio.app.features.player.metadata

/** A source-specific scope is required before embedded chapter facts can reach Scene Info. */
internal fun desktopSceneInfoScope(
    metadata: TimedMetadataScope?,
    source: String,
    sourceAvailable: Boolean,
): TimedMetadataScope? {
    val identity = metadata ?: return null
    if (!sourceAvailable || source.isBlank() || source.length > 16_384) return null
    if (!listOf(identity.mediaId, identity.mediaType, identity.videoId).all { it.isNotBlank() && it.length <= 1024 }) return null
    if (identity.mediaType.equals("series", ignoreCase = true) && identity.videoId == identity.mediaId) return null
    return runCatching { identity.copy(editionId = SceneBookmarkScope.sourceEdition(source)) }.getOrNull()
}

/** Prevents a completed provider request for the previous source from surviving a media change. */
internal fun desktopSceneInfoEntriesAt(
    timeline: TimedMetadataTimeline?,
    expectedScope: TimedMetadataScope?,
    positionMs: Long,
): List<SceneInfoEntry> = if (expectedScope == null || timeline?.scope != expectedScope) {
    emptyList()
} else {
    timeline.sceneInfoAt(positionMs)
}
