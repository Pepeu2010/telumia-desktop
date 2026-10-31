package com.nuvio.app.features.trakt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TraktCommentsCacheTest {
    @Test
    fun pageHitsRefreshRecencyAndOldestPageIsEvictedPerTitle() {
        val cache = cache(titleEntries = 2, pagesPerTitle = 2)
        cache.put("movie-a", 1, comments(1), pageCount = 5, itemCount = 500, nowMs = 0)
        cache.put("movie-a", 2, comments(2), pageCount = 5, itemCount = 500, nowMs = 0)

        assertEquals(comments(1), cache.get("movie-a", 1, nowMs = 1)?.items)
        cache.put("movie-a", 3, comments(3), pageCount = 5, itemCount = 500, nowMs = 2)

        assertNull(cache.get("movie-a", 2, nowMs = 3))
        assertEquals(comments(1), cache.get("movie-a", 1, nowMs = 3)?.items)
        assertEquals(comments(3), cache.get("movie-a", 3, nowMs = 3)?.items)
        assertEquals(2, cache.pageCount("movie-a"))
    }

    @Test
    fun titleHitsRefreshRecencyAndOldestTitleIsEvictedAtCapacity() {
        val cache = cache(titleEntries = 2, pagesPerTitle = 2)
        cache.put("movie-a", 1, comments(1), pageCount = 1, itemCount = 1, nowMs = 0)
        cache.put("movie-b", 1, comments(2), pageCount = 1, itemCount = 1, nowMs = 0)

        assertEquals(comments(1), cache.get("movie-a", 1, nowMs = 1)?.items)
        cache.put("movie-c", 1, comments(3), pageCount = 1, itemCount = 1, nowMs = 2)

        assertNull(cache.get("movie-b", 1, nowMs = 3))
        assertEquals(comments(1), cache.get("movie-a", 1, nowMs = 3)?.items)
        assertEquals(comments(3), cache.get("movie-c", 1, nowMs = 3)?.items)
        assertEquals(2, cache.titleCount())
    }

    @Test
    fun expiredTitlesAreRemovedAndNewPagesDoNotRetainTheirExpiredPages() {
        val cache = cache(titleEntries = 2, pagesPerTitle = 2, ttlMs = 10)
        cache.put("show-a", 1, comments(1), pageCount = 3, itemCount = 300, nowMs = 100)

        assertEquals(comments(1), cache.get("show-a", 1, nowMs = 110)?.items)
        assertNull(cache.get("show-a", 1, nowMs = 111))
        assertEquals(0, cache.titleCount())

        cache.put("show-a", 1, comments(1), pageCount = 3, itemCount = 300, nowMs = 200)
        cache.put("show-a", 2, comments(2), pageCount = 3, itemCount = 300, nowMs = 211)

        assertNull(cache.get("show-a", 1, nowMs = 211))
        assertEquals(comments(2), cache.get("show-a", 2, nowMs = 211)?.items)
        assertEquals(1, cache.pageCount("show-a"))
    }

    @Test
    fun concurrentPageWritesToOneTitleAreMergedWithoutExceedingThePageLimit() = runTest {
        val cache = cache(titleEntries = 1, pagesPerTitle = 400)

        (0 until 4).map { worker ->
            async(Dispatchers.Default) {
                repeat(100) { index ->
                    val page = worker * 100 + index
                    cache.put(
                        titleKey = "show-concurrent",
                        page = page,
                        items = comments(page),
                        pageCount = 400,
                        itemCount = 40_000,
                        nowMs = 0,
                    )
                }
            }
        }.awaitAll()

        assertEquals(1, cache.titleCount())
        assertEquals(400, cache.pageCount("show-concurrent"))
        (0 until 400).forEach { page ->
            assertEquals(comments(page), cache.get("show-concurrent", page, nowMs = 1)?.items)
        }
    }

    private fun cache(
        titleEntries: Int,
        pagesPerTitle: Int,
        ttlMs: Long = 100,
    ) = TraktCommentsCache(
        limits = TraktCommentsCacheLimits(titleEntries, pagesPerTitle),
        ttlMs = ttlMs,
    )

    private fun comments(id: Int) = listOf(
        TraktCommentReview(id = id.toLong(), authorDisplayName = "user-$id", comment = "comment-$id"),
    )
}
