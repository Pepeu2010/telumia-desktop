package com.nuvio.app.features.tracking

import java.time.Instant

internal actual object TrackingPlatformClock {
    actual fun nowEpochMs(): Long = System.currentTimeMillis()

    actual fun parseIsoDateTimeToEpochMs(value: String): Long? =
        runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()

    actual fun availableProcessors(): Int = Runtime.getRuntime().availableProcessors()
}
