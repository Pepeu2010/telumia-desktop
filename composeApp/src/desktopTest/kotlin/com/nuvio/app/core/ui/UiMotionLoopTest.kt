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

class UiMotionLoopTest {
    @Test
    fun sharedDecorativeLoopStopsForReducedAndOffAndResumesForFull() = runBlocking {
        val frameClock = BroadcastFrameClock()
        val recomposer = Recomposer(coroutineContext + frameClock)
        val composition = Composition(EmptyApplier(), recomposer)
        val runner = launch(frameClock) { recomposer.runRecomposeAndApplyChanges() }
        val mode = mutableStateOf(NavigationMotion.FULL)
        val screenActive = mutableStateOf(true)
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
                CompositionLocalProvider(
                    LocalUiMotion provides UiMotionPolicy(mode.value),
                    LocalScreenActive provides screenActive.value,
                ) {
                    progress = rememberUiMotionLoopFloat(
                        active = LocalScreenActive.current,
                        initialValue = 1f,
                        targetValue = 1.04f,
                        durationMillis = 2_000,
                        label = "testPulse",
                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                    )
                }
            }

            frame(0)
            frame(16)
            val fullFrame = progress.value
            frame(500)
            assertNotEquals(fullFrame, progress.value, "FULL must animate the decorative pulse")

            screenActive.value = false
            frame(516)
            frame(1_000)
            val inactiveFrame = progress.value
            frame(1_500)
            assertEquals(inactiveFrame, progress.value, "Inactive screens must keep decorative UI static")
            assertFalse(frameClock.hasAwaiters, "Inactive screens must release the infinite animation clock")

            screenActive.value = true
            frame(1_516)
            frame(2_000)
            val reactivatedFrame = progress.value
            frame(2_500)
            assertNotEquals(reactivatedFrame, progress.value, "FULL must resume when the screen becomes active")

            mode.value = NavigationMotion.REDUCED
            frame(2_516)
            frame(3_000)
            val reducedFrame = progress.value
            frame(3_500)
            assertEquals(reducedFrame, progress.value, "REDUCED must keep decorative UI static")
            assertFalse(frameClock.hasAwaiters, "REDUCED must release the infinite animation clock")

            mode.value = NavigationMotion.OFF
            frame(3_516)
            frame(4_000)
            val offFrame = progress.value
            frame(4_500)
            assertEquals(offFrame, progress.value, "OFF must keep decorative UI static")
            assertFalse(frameClock.hasAwaiters, "OFF must release the infinite animation clock")

            mode.value = NavigationMotion.FULL
            frame(4_516)
            frame(5_000)
            val resumedFrame = progress.value
            frame(5_500)
            assertNotEquals(resumedFrame, progress.value, "FULL must resume the pulse")
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
