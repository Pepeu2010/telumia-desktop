package com.nuvio.app.core.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.staticCompositionLocalOf

/** Optional parent hover source for native video surfaces that consume Compose pointer events. */
internal val LocalNativeVideoHoverInteractionSource =
    staticCompositionLocalOf<MutableInteractionSource?> { null }
