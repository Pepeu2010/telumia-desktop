package com.nuvio.app.features.details

internal sealed interface MetaTrailerPlaybackTarget {
    data class YouTube(
        val videoId: String,
        val url: String,
    ) : MetaTrailerPlaybackTarget

    data class External(
        val site: String,
        val url: String,
    ) : MetaTrailerPlaybackTarget
}

private val youtubeVideoIdPattern = Regex("^[A-Za-z0-9_-]{11}$")
private val videoIdPattern = Regex("^[A-Za-z0-9_-]+$")
private val absoluteHttpUrlPattern = Regex(
    "^https?://([^/?#]+)([^?#]*)(?:\\?([^#]*))?(?:#.*)?$",
    RegexOption.IGNORE_CASE,
)

/** Resolves a meta trailer according to its declared provider. */
internal fun resolveTrailerPlaybackTarget(trailer: MetaTrailer): MetaTrailerPlaybackTarget? {
    val key = trailer.key.trim()
    if (key.isBlank()) return null

    if (trailer.site.equals("YouTube", ignoreCase = true)) {
        val videoId = extractYouTubeVideoId(key) ?: return null
        val url = if (youtubeVideoIdPattern.matches(key)) {
            "https://www.youtube.com/watch?v=$videoId"
        } else {
            key
        }
        return MetaTrailerPlaybackTarget.YouTube(videoId = videoId, url = url)
    }

    val externalUrl = resolveExternalProviderUrl(trailer.site, key) ?: return null
    return MetaTrailerPlaybackTarget.External(site = trailer.site, url = externalUrl)
}

internal fun MetaTrailer.youtubePlaybackUrl(): String =
    requireNotNull((resolveTrailerPlaybackTarget(this) as? MetaTrailerPlaybackTarget.YouTube)?.url) {
        "Trailer is not a playable YouTube item"
    }

internal fun MetaTrailer.youtubeThumbnailUrl(): String? =
    (resolveTrailerPlaybackTarget(this) as? MetaTrailerPlaybackTarget.YouTube)
        ?.videoId
        ?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }

private fun resolveExternalProviderUrl(site: String, key: String): String? {
    if (absoluteHttpUrlPattern.matchEntire(key) != null) {
        val host = urlHost(key) ?: return null
        val expectedDomains = expectedProviderDomains(site)
        if (expectedDomains != null && expectedDomains.none { hostMatchesDomain(host, it) }) {
            return null
        }
        return key
    }

    return when (site.trim().lowercase()) {
        "vimeo" -> key.takeIf { it.all(Char::isDigit) }
            ?.let { "https://vimeo.com/$it" }
        "dailymotion" -> key.takeIf { videoIdPattern.matches(it) }
            ?.let { "https://www.dailymotion.com/video/$it" }
        else -> null
    }
}

private fun extractYouTubeVideoId(key: String): String? {
    if (youtubeVideoIdPattern.matches(key)) return key

    val match = absoluteHttpUrlPattern.matchEntire(key) ?: return null
    val host = urlHost(key) ?: return null
    if (!isYouTubeHost(host)) return null

    val path = match.groupValues[2].ifBlank { "/" }.trimEnd('/')
    val query = match.groupValues[3]
    val candidate = when {
        hostMatchesDomain(host, "youtu.be") -> path.trim('/').substringBefore('/')
        path == "/watch" -> query.split('&')
            .firstNotNullOfOrNull { parameter ->
                val name = parameter.substringBefore('=', missingDelimiterValue = "")
                val value = parameter.substringAfter('=', missingDelimiterValue = "")
                value.takeIf { name == "v" }
            }
        else -> {
            val segments = path.trim('/').split('/')
            when (segments.firstOrNull()?.lowercase()) {
                "embed", "shorts", "live" -> segments.getOrNull(1)
                else -> null
            }
        }
    }
    return candidate?.takeIf(youtubeVideoIdPattern::matches)
}

private fun urlHost(url: String): String? {
    val match = absoluteHttpUrlPattern.matchEntire(url) ?: return null
    val authority = match.groupValues[1]
    if (authority.isBlank() || '@' in authority) return null

    return authority.substringBefore(':')
        .trimEnd('.')
        .lowercase()
        .takeIf(String::isNotBlank)
}

private fun isYouTubeHost(host: String): Boolean =
    hostMatchesDomain(host, "youtube.com") || hostMatchesDomain(host, "youtu.be")

private fun expectedProviderDomains(site: String): List<String>? =
    when (site.trim().lowercase()) {
        "vimeo" -> listOf("vimeo.com")
        "dailymotion" -> listOf("dailymotion.com", "dai.ly")
        "facebook" -> listOf("facebook.com", "fb.watch")
        "instagram" -> listOf("instagram.com")
        "tiktok" -> listOf("tiktok.com")
        "twitch" -> listOf("twitch.tv")
        else -> null
    }

private fun hostMatchesDomain(host: String, domain: String): Boolean =
    host == domain || host.endsWith(".$domain")
