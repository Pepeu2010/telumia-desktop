package com.nuvio.app

import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

internal actual val isIos: Boolean = false
internal actual val isDesktop: Boolean = false
internal actual val isWindows: Boolean = false

@Composable
internal actual fun isTelevisionDevice(): Boolean {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val uiModeType = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
    return remember(context, uiModeType) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            uiModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
}

internal actual val supportsPosterNavigationMotion: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
