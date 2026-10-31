package com.nuvio.app.core.ui

import androidx.compose.ui.Modifier

internal actual fun Modifier.desktopBackNavigation(onBack: () -> Unit): Modifier = this

internal actual fun Modifier.consumeDesktopBackForwardButtons(): Modifier = this
