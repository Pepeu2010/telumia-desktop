package com.nuvio.app.features.updater

enum class UpdateChannel(val storedValue: String) {
    STABLE("stable");

    companion object {
        fun fromStoredValue(value: String?): UpdateChannel? =
            STABLE.takeIf { value.equals(STABLE.storedValue, ignoreCase = true) }

        @Suppress("UNUSED_PARAMETER")
        fun defaultForVersion(versionName: String): UpdateChannel = STABLE
    }
}
