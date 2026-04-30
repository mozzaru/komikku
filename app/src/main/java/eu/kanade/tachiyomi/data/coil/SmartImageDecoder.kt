package eu.kanade.tachiyomi.data.coil

import android.graphics.Bitmap
import eu.kanade.tachiyomi.data.cache.LocalFileBitmapCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import okio.BufferedSource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.decoder.ImageDecoder
import java.io.File

class SmartImageDecoder(
    private val localFileCache: LocalFileBitmapCache = LocalFileBitmapCache(),
) {

    suspend fun decodeFromFile(file: File): Bitmap? = withContext(Dispatchers.Default) {
        try {
            localFileCache.get(file)?.let {
                logcat(LogPriority.DEBUG) { "SmartDecoder: Using cached ${file.name}" }
                return@withContext it
            }

            val bitmap = file.inputStream().use { inputStream ->
                ImageDecoder.newInstance(inputStream)?.decode()
            }

            if (bitmap != null) {
                localFileCache.put(file, bitmap)
            }

            bitmap
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "SmartDecoder: Failed to decode ${file.name}" }
            null
        }
    }

    suspend fun decodeFromSource(source: BufferedSource): Bitmap? = withContext(Dispatchers.Default) {
        try {
            source.inputStream().use { inputStream ->
                ImageDecoder.newInstance(inputStream)?.decode()
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "SmartDecoder: Failed to decode from source" }
            null
        }
    }
}
