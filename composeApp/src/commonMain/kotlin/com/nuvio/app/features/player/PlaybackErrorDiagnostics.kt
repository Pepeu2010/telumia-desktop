package com.nuvio.app.features.player

import androidx.compose.runtime.Composable
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.player_diagnostic_error_code
import nuvio.composeapp.generated.resources.player_diagnostic_format_mime
import nuvio.composeapp.generated.resources.player_diagnostic_source_type
import nuvio.composeapp.generated.resources.player_diagnostic_stage
import nuvio.composeapp.generated.resources.player_diagnostics_title
import nuvio.composeapp.generated.resources.player_error_summary_cancelled
import nuvio.composeapp.generated.resources.player_error_summary_decode
import nuvio.composeapp.generated.resources.player_error_summary_http
import nuvio.composeapp.generated.resources.player_error_summary_network
import nuvio.composeapp.generated.resources.player_error_summary_timeout
import nuvio.composeapp.generated.resources.player_error_summary_unknown
import nuvio.composeapp.generated.resources.player_error_summary_unsupported_format
import org.jetbrains.compose.resources.stringResource

internal enum class PlaybackDiagnosticStage(val value: String) {
    SourceResolution("source-resolution"),
    PlayerStartup("player-startup"),
    Playback("playback"),
}

/** Only allowlisted playback facts are retained in a report. Raw URLs, headers, and error text are never copied. */
internal data class PlaybackErrorDiagnostics(
    val stage: PlaybackDiagnosticStage,
    val sourceType: String,
    val formatOrMime: String,
    val errorCode: String,
) {
    fun toSafeJson(): String =
        """{"stage":"${stage.value}","source_type":"$sourceType","format_or_mime":"$formatOrMime","error_code":"$errorCode"}"""
}

internal fun playbackErrorDiagnostics(
    stage: PlaybackDiagnosticStage,
    sourceUrl: String?,
    streamType: String?,
    responseHeaders: Map<String, String>?,
    errorMessage: String?,
): PlaybackErrorDiagnostics = PlaybackErrorDiagnostics(
    stage = stage,
    sourceType = safePlaybackSourceType(sourceUrl, streamType),
    formatOrMime = safePlaybackFormat(sourceUrl, streamType, responseHeaders),
    errorCode = playbackErrorCode(errorMessage),
)

/** Replaces arbitrary native text with an allowlisted code before Desktop forwards the error. */
internal fun safePlaybackErrorToken(message: String?): String? =
    message?.let(::playbackErrorCode)

internal fun playbackErrorCode(message: String?): String {
    val raw = message.orEmpty()
    val normalizedCode = raw.trim().uppercase()
    if (normalizedCode in SAFE_PLAYBACK_ERROR_CODES) return normalizedCode
    Regex("^HTTP_([45]\\d{2})$").matchEntire(normalizedCode)?.let { match ->
        return "HTTP_${match.groupValues[1]}"
    }

    val httpStatus = Regex(
        pattern = "\\bhttp(?:\\s+(?:status|error))?\\s*[:#=-]?\\s*([45]\\d{2})\\b",
        option = RegexOption.IGNORE_CASE,
    ).find(raw)?.groupValues?.getOrNull(1)
        ?: Regex(
            pattern = "\\bstatus(?:\\s+code)?\\s*[:#=-]?\\s*([45]\\d{2})\\b",
            option = RegexOption.IGNORE_CASE,
        ).find(raw)?.groupValues?.getOrNull(1)
    if (httpStatus != null) return "HTTP_$httpStatus"

    val normalized = raw.lowercase()
    return when {
        "timeout" in normalized || "timed out" in normalized -> "TIMEOUT"
        "cancelled" in normalized || "canceled" in normalized -> "CANCELLED"
        "unsupported format" in normalized || "unknown format" in normalized -> "UNSUPPORTED_FORMAT"
        listOf("decode", "demux", "codec").any(normalized::contains) -> "DECODE_ERROR"
        listOf("network", "connection", "resolve host", "socket", "unreachable").any(normalized::contains) ->
            "NETWORK_ERROR"
        else -> "UNKNOWN"
    }
}

@Composable
internal fun playbackErrorSummary(message: String?): String {
    val code = playbackErrorCode(message)
    return when {
        code.startsWith("HTTP_") -> stringResource(
            Res.string.player_error_summary_http,
            code.removePrefix("HTTP_"),
        )
        code == "TIMEOUT" -> stringResource(Res.string.player_error_summary_timeout)
        code == "CANCELLED" -> stringResource(Res.string.player_error_summary_cancelled)
        code == "UNSUPPORTED_FORMAT" -> stringResource(Res.string.player_error_summary_unsupported_format)
        code == "NETWORK_ERROR" -> stringResource(Res.string.player_error_summary_network)
        code == "DECODE_ERROR" -> stringResource(Res.string.player_error_summary_decode)
        else -> stringResource(Res.string.player_error_summary_unknown)
    }
}

@Composable
internal fun playbackDiagnosticTitle(): String = stringResource(Res.string.player_diagnostics_title)

@Composable
internal fun playbackDiagnosticStageLabel(value: String): String =
    stringResource(Res.string.player_diagnostic_stage, value)

@Composable
internal fun playbackDiagnosticSourceTypeLabel(value: String): String =
    stringResource(Res.string.player_diagnostic_source_type, value)

@Composable
internal fun playbackDiagnosticFormatMimeLabel(value: String): String =
    stringResource(Res.string.player_diagnostic_format_mime, value)

@Composable
internal fun playbackDiagnosticErrorCodeLabel(value: String): String =
    stringResource(Res.string.player_diagnostic_error_code, value)

private val SAFE_PLAYBACK_ERROR_CODES = setOf(
    "CANCELLED",
    "DECODE_ERROR",
    "NETWORK_ERROR",
    "TIMEOUT",
    "UNKNOWN",
    "UNSUPPORTED_FORMAT",
)

private fun safePlaybackSourceType(sourceUrl: String?, streamType: String?): String {
    val normalizedType = streamType?.trim()?.lowercase()
    if (normalizedType in setOf("p2p", "torrent", "magnet")) return "p2p"

    val scheme = sourceUrl.orEmpty()
        .substringBefore(':', missingDelimiterValue = "")
        .lowercase()
    return when (scheme) {
        "http", "https" -> "http"
        "file" -> "file"
        "magnet", "torrent" -> "p2p"
        else -> "unknown"
    }
}

private fun safePlaybackFormat(
    sourceUrl: String?,
    streamType: String?,
    responseHeaders: Map<String, String>?,
): String {
    val contentType = responseHeaders.orEmpty()
        .entries
        .firstOrNull { (name, _) -> name.equals("content-type", ignoreCase = true) }
        ?.value
        ?.substringBefore(';')
        ?.trim()
        ?.lowercase()
        ?.takeIf { it in SAFE_MEDIA_MIME_TYPES }
    if (contentType != null) return contentType

    val normalizedType = streamType?.trim()?.lowercase()
    if (normalizedType in SAFE_MEDIA_MIME_TYPES) return normalizedType.orEmpty()
    when (normalizedType) {
        "hls", "m3u8" -> return "hls"
        "dash", "mpd" -> return "dash"
    }

    val path = sourceUrl.orEmpty()
        .substringBefore('?')
        .substringBefore('#')
        .substringAfterLast('/')
        .substringAfterLast('\\')
    val extension = path.substringAfterLast('.', missingDelimiterValue = "")
        .lowercase()
        .takeIf { it in SAFE_MEDIA_EXTENSIONS }
        ?: return "unknown"
    return when (extension) {
        "m3u8" -> "hls (.m3u8)"
        "mpd" -> "dash (.mpd)"
        else -> ".$extension"
    }
}

private val SAFE_MEDIA_MIME_TYPES = setOf(
    "application/dash+xml",
    "application/vnd.apple.mpegurl",
    "application/x-mpegurl",
    "video/mp2t",
    "video/mp4",
    "video/quicktime",
    "video/webm",
    "video/x-matroska",
    "video/x-msvideo",
)

private val SAFE_MEDIA_EXTENSIONS = setOf(
    "avi",
    "m2ts",
    "m3u8",
    "m4v",
    "mkv",
    "mov",
    "mp4",
    "mpd",
    "mpeg",
    "mpg",
    "ts",
    "webm",
)
