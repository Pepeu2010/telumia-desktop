package com.nuvio.app.features.updater

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class UpdatePreferences(versionName: String = AppUpdaterPlatform.currentVersionName) {
    private val storedChannel = UpdateChannel.fromStoredValue(AppUpdaterPlatform.getUpdateChannel())
    private val defaultChannel = storedChannel ?: UpdateChannel.defaultForVersion(versionName)
    private val _channel = MutableStateFlow(defaultChannel)
    val channel = _channel.asStateFlow()

    init {
        if (AppUpdaterPlatform.isSupported && (storedChannel == null || storedChannel != defaultChannel)) {
            AppUpdaterPlatform.setUpdateChannel(_channel.value.storedValue)
        }
    }

    fun setChannel(channel: UpdateChannel) {
        val selectedChannel = UpdateChannel.STABLE
        if (_channel.value == selectedChannel) return
        AppUpdaterPlatform.setUpdateChannel(selectedChannel.storedValue)
        AppUpdaterPlatform.setIgnoredTag(null)
        _channel.value = selectedChannel
    }

    companion object {
        val shared by lazy { UpdatePreferences() }
    }
}
