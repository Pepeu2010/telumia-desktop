package com.nuvio.app.features.streams

enum class StreamAutoPlayMode {
    MANUAL,
    FIRST_STREAM,
    REGEX_MATCH,
    BEST_QUALITY,
    BALANCED,
    DATA_SAVER;

    val usesSourceScore: Boolean
        get() = this == BEST_QUALITY || this == BALANCED || this == DATA_SAVER
}

enum class StreamAutoPlaySource {
    ALL_SOURCES,
    INSTALLED_ADDONS_ONLY,
    ENABLED_PLUGINS_ONLY,
}
