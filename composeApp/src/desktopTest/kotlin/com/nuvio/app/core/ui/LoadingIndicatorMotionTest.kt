package com.nuvio.app.core.ui

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class LoadingIndicatorMotionTest {
    @Test
    fun reducedAndOffMotionStopLoadingFramesAndFullMotionResumesThem() = runBlocking {
        val frameClock = BroadcastFrameClock()
        val recomposer = Recomposer(coroutineContext + frameClock)
        val composition = Composition(EmptyApplier(), recomposer)
        val runner = launch(frameClock) { recomposer.runRecomposeAndApplyChanges() }
        val mode = mutableStateOf(NavigationMotion.FULL)
        lateinit var progress: State<Float>

        suspend fun frame(millis: Long) {
            Snapshot.sendApplyNotifications()
            yield()
            frameClock.sendFrame(millis * 1_000_000L)
            yield()
            Snapshot.sendApplyNotifications()
            yield()
        }

        try {
            composition.setContent {
                CompositionLocalProvider(LocalUiMotion provides UiMotionPolicy(mode.value)) {
                    progress = rememberLoadingIndicatorFrame(active = true)
                }
            }

            frame(0)
            frame(16)
            frame(32)
            val fullFrame = progress.value
            frame(300)
            assertNotEquals(fullFrame, progress.value, "FULL must keep the existing spinner animation")

            mode.value = NavigationMotion.REDUCED
            frame(316)
            frame(332)
            frame(600)
            val reducedFrame = progress.value
            frame(900)
            assertEquals(reducedFrame, progress.value, "REDUCED must leave a static loading arc")
            assertFalse(frameClock.hasAwaiters, "REDUCED must release the infinite animation clock")

            mode.value = NavigationMotion.OFF
            frame(916)
            frame(932)
            frame(1_200)
            val offFrame = progress.value
            frame(1_500)
            assertEquals(offFrame, progress.value, "OFF must leave a static loading arc")
            assertFalse(frameClock.hasAwaiters, "OFF must release the infinite animation clock")

            mode.value = NavigationMotion.FULL
            frame(1_516)
            frame(1_532)
            val resumedFrame = progress.value
            frame(1_800)
            assertNotEquals(resumedFrame, progress.value, "FULL must restart the spinner when re-enabled")
        } finally {
            composition.dispose()
            recomposer.cancel()
            runner.join()
        }
    }

    private class EmptyApplier : AbstractApplier<Unit>(Unit) {
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun onClear() = Unit
    }
}
