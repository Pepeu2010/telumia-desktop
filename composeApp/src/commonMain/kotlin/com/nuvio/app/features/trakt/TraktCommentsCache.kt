package com.nuvio.app.features.trakt

import com.nuvio.app.features.details.MetaDetailsCacheLimits
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/** Initial title/page budgets reuse the physical-memory tiers already used for details metadata. */
internal data class TraktCommentsCacheLimits(
    val titleEntries: Int,
    val pagesPerTitle: Int,
) {
    init {
        require(titleEntries > 0) { "Comments title cache capacity must be positive" }
        require(pagesPerTitle > 0) { "Comments pages per title must be positive" }
    }

    companion object {
        val DEFAULT = forTitleEntries(MetaDetailsCacheLimits.DEFAULT.entries)

        fun forPhysicalMemoryBytes(totalMemoryBytes: Long?): TraktCommentsCacheLimits =
            forTitleEntries(MetaDetailsCacheLimits.forPhysicalMemoryBytes(totalMemoryBytes).entries)

        private fun forTitleEntries(titleEntries: Int) = TraktCommentsCacheLimits(
            titleEntries = titleEntries,
            pagesPerTitle = 1 + titleEntries / 8,
        )
    }
}

/** Thread-safe TTL/LRU for Trakt comments, bounded across titles and pages within each title. */
internal class TraktCommentsCache(
    private val limits: TraktCommentsCacheLimits,
    private val ttlMs: Long,
) {
    private data class Entry(
        val pages: LinkedHashMap<Int, List<TraktCommentReview>>,
        val pageCount: Int,
        val itemCount: Int,
        val updatedAtMs: Long,
    )

    private val lock = SynchronizedObject()
    private val titles = linkedMapOf<String, Entry>()

    init {
        require(ttlMs > 0L) { "Comments cache TTL must be positive" }
    }

    fun get(titleKey: String, page: Int, nowMs: Long): TraktCommentsPage? = synchronized(lock) {
        val entry = titles[titleKey] ?: return@synchronized null
        if (nowMs - entry.updatedAtMs > ttlMs) {
            titles.remove(titleKey)
            return@synchronized null
        }

        val items = entry.pages.remove(page) ?: return@synchronized null
        entry.pages[page] = items
        titles.remove(titleKey)
        titles[titleKey] = entry
        TraktCommentsPage(
            items = items,
            currentPage = page,
            pageCount = entry.pageCount,
            itemCount = entry.itemCount,
        )
    }

    fun put(
        titleKey: String,
        page: Int,
        items: List<TraktCommentReview>,
        pageCount: Int,
        itemCount: Int,
        nowMs: Long,
    ) = synchronized(lock) {
        val previous = titles.remove(titleKey)?.takeUnless { nowMs - it.updatedAtMs > ttlMs }
        val pages = linkedMapOf<Int, List<TraktCommentReview>>().apply {
            putAll(previous?.pages.orEmpty())
            remove(page)
            put(page, items.toList())
        }
        while (pages.size > limits.pagesPerTitle) {
            pages.remove(pages.keys.first())
        }

        titles[titleKey] = Entry(
            pages = pages,
            pageCount = pageCount,
            itemCount = itemCount,
            updatedAtMs = nowMs,
        )
        while (titles.size > limits.titleEntries) {
            titles.remove(titles.keys.first())
        }
    }

    fun remove(titleKey: String) {
        synchronized(lock) { titles.remove(titleKey) }
    }

    fun clear() = synchronized(lock) { titles.clear() }

    fun titleCount(): Int = synchronized(lock) { titles.size }

    fun pageCount(titleKey: String): Int = synchronized(lock) { titles[titleKey]?.pages?.size ?: 0 }
}
