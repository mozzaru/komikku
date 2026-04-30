package eu.kanade.tachiyomi.ui.reader.model

import android.graphics.Bitmap
import android.util.Log
import java.util.LinkedHashSet

/**
 * Manage visual cache [ReaderPage.decodedBitmap] to avoid OOM.
 * Limits the number of bitmaps saved (default 3 pages).
 */
object PageCacheManager {
    private const val MAX_CACHED_PAGES = 3
    private val cachedPages = LinkedHashSet<ReaderPage>(MAX_CACHED_PAGES)

    /**
     * Stores bitmaps for pages. If the cache is full, the oldest pages will be recycled.
     */
    fun cacheBitmap(page: ReaderPage, bitmap: Bitmap) {
        // Delete old pages if they are full
        if (cachedPages.size >= MAX_CACHED_PAGES) {
            val oldest = cachedPages.first()
            oldest.clearDecodedBitmap()
            cachedPages.remove(oldest)
            Log.d(
                "ReaderCache",
                "Evicted page ${oldest.index} from cache",
            )
        }
        cachedPages.add(page)
        page.decodedBitmap = bitmap
        Log.d(
            "ReaderCache",
            "Cached bitmap for page ${page.index}, " +
                "total cached=${cachedPages.size}",
        )
    }

    /**
     * Deleting certain pages from the cache (e.g. when the page is no longer needed).
     */
    fun evict(page: ReaderPage) {
        if (cachedPages.remove(page)) {
            page.clearDecodedBitmap()
            Log.d("ReaderCache", "Manually evicted page ${page.index}")
        }
    }

    /**
     * Clears the entire cache, for example when the reader is completely closed.
     */
    fun evictAll() {
        cachedPages.forEach { it.clearDecodedBitmap() }
        cachedPages.clear()
        Log.d("ReaderCache", "Cleared all cached bitmaps")
    }
}
