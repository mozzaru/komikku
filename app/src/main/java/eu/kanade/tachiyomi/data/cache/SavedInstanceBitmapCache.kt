package eu.kanade.tachiyomi.data.cache

import android.graphics.Bitmap
import android.os.Bundle
import androidx.collection.LruCache
import logcat.LogPriority
import logcat.logcat

class SavedInstanceBitmapCache {
    private val persistentCache = mutableMapOf<String, Bitmap>()

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024 / 1024).toInt()

    private val lruCache = object : LruCache<String, Bitmap>((maxMemory / 4).coerceAtMost(50)) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.width * value.height * 4) / 1024
        }
    }

    fun put(key: String, bitmap: Bitmap, isPersistent: Boolean = false) {
        if (isPersistent) {
            persistentCache[key] = bitmap
            logcat(LogPriority.DEBUG) { "SavedInstanceCache PUT (PERSISTENT): $key" }
        }
        lruCache.put(key, bitmap)
    }

    fun get(key: String): Bitmap? {
        persistentCache[key]?.let {
            logcat(LogPriority.DEBUG) { "SavedInstanceCache HIT (PERSISTENT): $key" }
            return it
        }

        return lruCache.get(key).also {
            if (it != null) {
                logcat(LogPriority.DEBUG) { "SavedInstanceCache HIT (LRU): $key" }
            }
        }
    }

    fun remove(key: String) {
        persistentCache.remove(key)
        lruCache.remove(key)
    }

    fun clear() {
        persistentCache.clear()
        lruCache.evictAll()
    }
}
