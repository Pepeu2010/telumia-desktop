package com.nuvio.app.core.storage

import java.util.concurrent.Executor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MediaCacheOccupancyMonitorTest {
    private class QueuedExecutor : Executor {
        private val tasks = mutableListOf<Runnable>()
        override fun execute(command: Runnable) { tasks.add(command) }
        fun runNext() = tasks.removeAt(0).run()
        fun size() = tasks.size
    }

    @Test fun firstAndStaleSnapshotsScheduleBoundedScansWithoutBlockingCaller() {
        val executor = QueuedExecutor()
        var now = 1_000L
        var scans = 0
        var updates = 0
        val monitor = MediaCacheOccupancyMonitor(
            scan = { scans++; MediaCacheOccupancy(scans * 10L, scans, complete = true) },
            executor = executor,
            onSnapshotUpdated = { updates++ },
            clockMillis = { now },
            refreshIntervalMillis = 100L,
        )

        assertEquals(MediaCacheOccupancy(0, 0, complete = false), monitor.current())
        assertEquals(0, scans)
        assertEquals(1, executor.size())

        executor.runNext()
        assertEquals(1, updates)
        assertEquals(MediaCacheOccupancy(10, 1, complete = true), monitor.current())
        assertEquals(1, scans)

        now += 100L
        assertEquals(MediaCacheOccupancy(10, 1, complete = true), monitor.current())
        assertEquals(1, executor.size())
        executor.runNext()
        assertEquals(2, updates)
        assertEquals(MediaCacheOccupancy(20, 2, complete = true), monitor.current())
        assertEquals(2, scans)
        assertEquals(0, executor.size())
    }

    @Test fun failedScanRetainsKnownBytesAndMarksTheSnapshotIncomplete() {
        val executor = QueuedExecutor()
        var now = 0L
        var shouldFail = false
        val monitor = MediaCacheOccupancyMonitor(
            scan = {
                if (shouldFail) error("filesystem unavailable")
                MediaCacheOccupancy(41, 3, complete = true)
            },
            executor = executor,
            clockMillis = { now },
            refreshIntervalMillis = 10L,
        )

        monitor.current()
        executor.runNext()
        assertEquals(MediaCacheOccupancy(41, 3, complete = true), monitor.current())
        now = 10L
        shouldFail = true
        monitor.current()
        executor.runNext()

        val afterFailure = monitor.current()
        assertEquals(41L, afterFailure.bytes)
        assertEquals(3, afterFailure.visitedEntries)
        assertFalse(afterFailure.complete)
        assertEquals(0, executor.size())
    }
}
