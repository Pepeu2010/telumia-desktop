package com.nuvio.app.core.ui

import androidx.compose.ui.Modifier

internal expect fun Modifier.desktopBackNavigation(onBack: () -> Unit): Modifier

internal expect fun Modifier.consumeDesktopBackForwardButtons(): Modifier
