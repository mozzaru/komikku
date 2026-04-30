package eu.kanade.tachiyomi.data.cache

import android.graphics.Bitmap
import androidx.collection.LruCache
import logcat.LogPriority
import logcat.logcat
import java.io.File

class LocalFileBitmapCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024 / 1024).toInt()
    private val cacheSize = (maxMemory / 3).coerceAtMost(100)

    private val cache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.width * value.height * 4) / 1024
        }
    }

    fun get(file: File): Bitmap? {
        return cache.get(file.absolutePath).also {
            if (it != null) {
                logcat(LogPriority.DEBUG) { "LocalFileBitmapCache HIT: ${file.name}" }
            }
        }
    }

    fun put(file: File, bitmap: Bitmap) {
        cache.put(file.absolutePath, bitmap)
        logcat(LogPriority.DEBUG) { "LocalFileBitmapCache PUT: ${file.name}" }
    }

    fun remove(file: File) {
        cache.remove(file.absolutePath)
    }

    fun clear() {
        cache.evictAll()
    }
}
