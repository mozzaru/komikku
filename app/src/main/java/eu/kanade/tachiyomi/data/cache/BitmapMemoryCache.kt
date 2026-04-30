package eu.kanade.tachiyomi.data.cache

import android.graphics.Bitmap
import androidx.collection.LruCache
import logcat.LogPriority
import logcat.logcat

class BitmapMemoryCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024 / 1024).toInt()
    private val cacheSize = (maxMemory / 4).coerceAtMost(50)

    private val cache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.width * value.height * 4) / 1024
        }
    }

    fun get(key: String): Bitmap? {
        return cache.get(key).also {
            if (it != null) {
                logcat(LogPriority.DEBUG) { "BitmapMemoryCache HIT: $key" }
            }
        }
    }

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
        logcat(LogPriority.DEBUG) { "BitmapMemoryCache PUT: $key" }
    }

    fun remove(key: String) {
        cache.remove(key)
    }

    fun clear() {
        cache.evictAll()
    }
}
