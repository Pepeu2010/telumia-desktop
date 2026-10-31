package com.nuvio.app.core.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun Modifier.desktopBackNavigation(onBack: () -> Unit): Modifier =
    pointerInput(onBack) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press &&
                    event.button == PointerButton.Back &&
                    event.changes.none { it.isConsumed }
                ) {
                    event.changes.forEach { it.consume() }
                    onBack()
                }
            }
        }
    }

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun Modifier.consumeDesktopBackForwardButtons(): Modifier =
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type == PointerEventType.Press &&
                    (event.button == PointerButton.Back || event.button == PointerButton.Forward)
                ) {
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }
