package com.nuvio.app

import androidx.compose.runtime.Composable

class DesktopPlatform : Platform {
    override val name: String = "Desktop ${System.getProperty("os.name").orEmpty()}".trim()
}

actual fun getPlatform(): Platform = DesktopPlatform()

internal actual val isIos: Boolean = false
internal actual val isDesktop: Boolean = true
internal actual val isWindows: Boolean = System.getProperty("os.name").orEmpty().lowercase().contains("win")

@Composable
internal actual fun isTelevisionDevice(): Boolean = false

internal actual val supportsPosterNavigationMotion: Boolean = false
