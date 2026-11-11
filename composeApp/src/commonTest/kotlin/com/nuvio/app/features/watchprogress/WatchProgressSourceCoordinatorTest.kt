package com.nuvio.app.features.watchprogress

import com.nuvio.app.features.tracking.WatchProgressSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

class WatchProgressSourceCoordinatorTest {
    @Test
    fun `serialized transition uses latest source for the same profile`() {
        val queued = context(source = WatchProgressSource.NUVIO_SYNC)
        val latest = context(source = WatchProgressSource.MDBLIST)

        assertEquals(
            latest,
            resolveSerializedWatchProgressContext(
                queuedContext = queued,
                currentContext = latest,
            ),
        )
        assertEquals(
            null,
            resolveSerializedWatchProgressContext(
                queuedContext = queued,
                currentContext = latest.copy(profileId = 2),
            ),
        )
    }

    @Test
    fun `source transition invalidates target cache and refreshes both read models`() = runBlocking {
        val invalidations = mutableListOf<Pair<Int, WatchProgressSource>>()
        val activatedProgress = mutableListOf<WatchProgressSource>()
        val activatedWatched = mutableListOf<WatchProgressSource>()
        val progressRefreshes = mutableListOf<RefreshCall>()
        val watchedRefreshes = mutableListOf<RefreshCall>()
        val runner = WatchProgressSourceTransitionRunner(
            invalidateCache = { profileId, source -> invalidations += profileId to source },
            activateProgressSource = activatedProgress::add,
            activateWatchedSource = activatedWatched::add,
            refreshProgress = { profileId, source, sourceChanged, force ->
                progressRefreshes += RefreshCall(profileId, source, sourceChanged, force)
                true
            },
            refreshWatched = { profileId, source, force ->
                watchedRefreshes += RefreshCall(profileId, source, sourceChanged = false, force = force)
                true
            },
        )

        runner.transition(
            context = context(source = WatchProgressSource.NUVIO_SYNC),
            refreshIfUnchanged = false,
            forceSnapshot = false,
        )
        invalidations.clear()
        activatedProgress.clear()
        activatedWatched.clear()
        progressRefreshes.clear()
        watchedRefreshes.clear()

        val result = runner.transition(
            context = context(source = WatchProgressSource.MDBLIST),
            refreshIfUnchanged = true,
            forceSnapshot = true,
        )

        assertTrue(result.succeeded)
        assertEquals(listOf(1 to WatchProgressSource.MDBLIST), invalidations)
        assertEquals(listOf(WatchProgressSource.MDBLIST), activatedProgress)
        assertEquals(listOf(WatchProgressSource.MDBLIST), activatedWatched)
        assertEquals(
            listOf(RefreshCall(1, WatchProgressSource.MDBLIST, sourceChanged = true, force = true)),
            progressRefreshes,
        )
        assertEquals(
            listOf(RefreshCall(1, WatchProgressSource.MDBLIST, sourceChanged = false, force = true)),
            watchedRefreshes,
        )
    }

    @Test
    fun `first coordinated transition detects a different already applied source`() = runBlocking {
        val invalidations = mutableListOf<Pair<Int, WatchProgressSource>>()
        var sourceChangedAtRefresh = false
        val runner = WatchProgressSourceTransitionRunner(
            currentAppliedSource = { WatchProgressSource.NUVIO_SYNC },
            invalidateCache = { profileId, source -> invalidations += profileId to source },
            activateProgressSource = {},
            activateWatchedSource = {},
            refreshProgress = { _, _, sourceChanged, _ ->
                sourceChangedAtRefresh = sourceChanged
                true
            },
            refreshWatched = { _, _, _ -> true },
        )

        runner.transition(
            context = context(source = WatchProgressSource.MDBLIST),
            refreshIfUnchanged = false,
            forceSnapshot = true,
        )

        assertTrue(sourceChangedAtRefresh)
        assertEquals(listOf(1 to WatchProgressSource.MDBLIST), invalidations)
    }

    @Test
    fun `same observed context does not refetch without force`() = runBlocking {
        var refreshCount = 0
        var invalidationCount = 0
        val runner = runner(
            onInvalidate = { invalidationCount += 1 },
            onRefresh = { refreshCount += 1 },
        )
        val context = context(source = WatchProgressSource.NUVIO_SYNC)

        runner.transition(context = context, refreshIfUnchanged = false, forceSnapshot = false)
        runner.transition(context = context, refreshIfUnchanged = false, forceSnapshot = false)

        assertEquals(2, refreshCount, "the initial transition refreshes progress and watched once")
        assertEquals(0, invalidationCount)
    }

    @Test
    fun `authentication change refreshes same effective source without invalidating it`() = runBlocking {
        var refreshCount = 0
        var invalidationCount = 0
        val runner = runner(
            onInvalidate = { invalidationCount += 1 },
            onRefresh = { refreshCount += 1 },
        )

        runner.transition(
            context = context(
                source = WatchProgressSource.NUVIO_SYNC,
                isNuvioAuthenticated = false,
            ),
            refreshIfUnchanged = false,
            forceSnapshot = false,
        )
        runner.transition(
            context = context(
                source = WatchProgressSource.NUVIO_SYNC,
                isNuvioAuthenticated = true,
            ),
            refreshIfUnchanged = false,
            forceSnapshot = false,
        )

        assertEquals(4, refreshCount)
        assertEquals(0, invalidationCount)
    }

    @Test
    fun `partial backend failure is surfaced`() = runBlocking {
        val runner = WatchProgressSourceTransitionRunner(
            invalidateCache = { _, _ -> },
            activateProgressSource = {},
            activateWatchedSource = {},
            refreshProgress = { _, _, _, _ -> true },
            refreshWatched = { _, _, _ -> false },
        )

        val result = runner.transition(
            context = context(source = WatchProgressSource.MDBLIST),
            refreshIfUnchanged = true,
            forceSnapshot = true,
        )

        assertTrue(result.progressRefreshed)
        assertFalse(result.watchedHistoryRefreshed)
        assertFalse(result.succeeded)
    }

    @Test
    fun `failed transition retries the same observed context`() = runBlocking {
        var watchedAttempts = 0
        val runner = WatchProgressSourceTransitionRunner(
            invalidateCache = { _, _ -> },
            activateProgressSource = {},
            activateWatchedSource = {},
            refreshProgress = { _, _, _, _ -> true },
            refreshWatched = { _, _, _ ->
                watchedAttempts += 1
                watchedAttempts > 1
            },
        )
        val context = context(source = WatchProgressSource.MDBLIST)

        val first = runner.transition(
            context = context,
            refreshIfUnchanged = false,
            forceSnapshot = true,
        )
        val retry = runner.transition(
            context = context,
            refreshIfUnchanged = false,
            forceSnapshot = true,
        )

        assertFalse(first.succeeded)
        assertTrue(retry.succeeded)
        assertEquals(2, watchedAttempts)
    }

    @Test
    fun `reverting after a failed source switch reactivates and refreshes the last successful source`() = runBlocking {
        var appliedSource = WatchProgressSource.NUVIO_SYNC
        val invalidations = mutableListOf<WatchProgressSource>()
        val refreshedSources = mutableListOf<WatchProgressSource>()
        var failMdbListWatchedRefresh = true
        val runner = WatchProgressSourceTransitionRunner(
            currentAppliedSource = { appliedSource },
            invalidateCache = { _, source -> invalidations += source },
            activateProgressSource = { source -> appliedSource = source },
            activateWatchedSource = {},
            refreshProgress = { _, source, _, _ ->
                refreshedSources += source
                true
            },
            refreshWatched = { _, source, _ ->
                refreshedSources += source
                source != WatchProgressSource.MDBLIST || !failMdbListWatchedRefresh
            },
        )

        assertTrue(
            runner.transition(
                context = context(source = WatchProgressSource.NUVIO_SYNC),
                refreshIfUnchanged = true,
                forceSnapshot = true,
            ).succeeded,
        )
        assertFalse(
            runner.transition(
                context = context(source = WatchProgressSource.MDBLIST),
                refreshIfUnchanged = false,
                forceSnapshot = true,
            ).succeeded,
        )

        failMdbListWatchedRefresh = false
        val reverted = runner.transition(
            context = context(source = WatchProgressSource.NUVIO_SYNC),
            refreshIfUnchanged = false,
            forceSnapshot = true,
        )

        assertTrue(reverted.succeeded)
        assertEquals(WatchProgressSource.NUVIO_SYNC, appliedSource)
        assertEquals(
            listOf(WatchProgressSource.MDBLIST, WatchProgressSource.NUVIO_SYNC),
            invalidations,
        )
        assertEquals(
            listOf(
                WatchProgressSource.NUVIO_SYNC,
                WatchProgressSource.NUVIO_SYNC,
                WatchProgressSource.MDBLIST,
                WatchProgressSource.MDBLIST,
                WatchProgressSource.NUVIO_SYNC,
                WatchProgressSource.NUVIO_SYNC,
            ),
            refreshedSources,
        )
    }

    @Test
    fun `account reset rejects an in flight transition and forgets its context`() = runBlocking {
        val refreshStarted = CompletableDeferred<Unit>()
        val releaseRefresh = CompletableDeferred<Unit>()
        var refreshCount = 0
        val runner = WatchProgressSourceTransitionRunner(
            invalidateCache = { _, _ -> },
            activateProgressSource = {},
            activateWatchedSource = {},
            refreshProgress = { _, _, _, _ ->
                refreshCount += 1
                refreshStarted.complete(Unit)
                releaseRefresh.await()
                true
            },
            refreshWatched = { _, _, _ -> true },
        )
        val context = context(source = WatchProgressSource.MDBLIST)
        val staleTransition = async {
            runner.transition(
                context = context,
                refreshIfUnchanged = false,
                forceSnapshot = true,
            )
        }

        refreshStarted.await()
        runner.reset()
        releaseRefresh.complete(Unit)
        assertFailsWith<CancellationException> { staleTransition.await() }

        runner.transition(
            context = context,
            refreshIfUnchanged = false,
            forceSnapshot = true,
        )
        assertEquals(2, refreshCount)
    }

    private fun runner(
        onInvalidate: () -> Unit,
        onRefresh: () -> Unit,
    ): WatchProgressSourceTransitionRunner = WatchProgressSourceTransitionRunner(
        invalidateCache = { _, _ -> onInvalidate() },
        activateProgressSource = {},
        activateWatchedSource = {},
        refreshProgress = { _, _, _, _ ->
            onRefresh()
            true
        },
        refreshWatched = { _, _, _ ->
            onRefresh()
            true
        },
    )

    private fun context(
        source: WatchProgressSource,
        isNuvioAuthenticated: Boolean = true,
    ): WatchProgressSourceContext = WatchProgressSourceContext(
        profileId = 1,
        requestedSource = source,
        effectiveSource = source,
        isNuvioAuthenticated = isNuvioAuthenticated,
    )

    private data class RefreshCall(
        val profileId: Int,
        val source: WatchProgressSource,
        val sourceChanged: Boolean,
        val force: Boolean,
    )
}
