package com.nuvio.app.core.format

import androidx.compose.ui.text.intl.Locale
import com.nuvio.app.core.time.parseEpisodeReleaseLocalDate

fun formatReleaseDateForDisplay(raw: String): String =
    formatReleaseDate(normalizePartialReleaseYear(raw), includeYear = true)

fun formatReleaseDateWithoutYear(raw: String): String =
    formatReleaseDate(normalizePartialReleaseYear(raw), includeYear = false)

private fun normalizePartialReleaseYear(raw: String): String {
    val value = raw.trim()
    return if (value.length == 5 && value.take(4).all(Char::isDigit) && value.last() in "-/.") {
        value.dropLast(1)
    } else {
        value
    }
}

internal fun formatReleaseDate(
    raw: String,
    includeYear: Boolean,
    localeTag: String = Locale.current.toLanguageTag(),
): String {
    val date = parseEpisodeReleaseLocalDate(raw) ?: return raw
    return formatCalendarDate(date, localeTag, includeYear)
}

internal expect fun formatCalendarDate(isoDate: String, localeTag: String, includeYear: Boolean): String

/**
 * Parses a release/air string (ISO date, year-only, or timestamp prefix) for compact UI (e.g. year chips).
 */
fun extractReleaseYearForDisplay(raw: String): Int? {
    val t = raw.trim()
    if (t.isEmpty()) return null
    if (t.length == 4 && t.all { it.isDigit() }) {
        return t.toIntOrNull()?.takeIf { it in 1000..9999 }
    }
    val datePart = parseEpisodeReleaseLocalDate(t) ?: return null
    val yearStr = datePart.split('-').firstOrNull() ?: return null
    return yearStr.toIntOrNull()?.takeIf { it in 1000..9999 }
}
