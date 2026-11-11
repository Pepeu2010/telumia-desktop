package com.nuvio.app.features.tracking

import java.time.Instant
import java.time.format.DateTimeParseException

internal actual object TrackingPlatformClock {
    actual fun nowEpochMs(): Long = System.currentTimeMillis()

    actual fun parseIsoDateTimeToEpochMs(value: String): Long? =
        try {
            Instant.parse(value).toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }

    actual fun availableProcessors(): Int =
        Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
}
