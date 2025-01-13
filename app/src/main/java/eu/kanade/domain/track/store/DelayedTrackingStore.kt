package eu.kanade.domain.track.store

import android.content.Context
import androidx.core.content.edit
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

class DelayedTrackingStore(context: Context) {

    /**
     * Preference file where queued tracking updates are stored.
     */
    private val preferences = context.getSharedPreferences("tracking_queue", Context.MODE_PRIVATE)

    fun add(trackId: Long, lastEpisodeRead: Double) {
        val previousLastEpisodeRead = preferences.getFloat(trackId.toString(), 0f)
        if (lastEpisodeRead > previousLastEpisodeRead) {
            logcat(LogPriority.DEBUG) { "Queuing track item: $trackId, last episode read: $lastEpisodeRead" }
            preferences.edit {
                putFloat(trackId.toString(), lastEpisodeRead.toFloat())
            }
        }
    }

    fun remove(trackId: Long) {
        preferences.edit {
            remove(trackId.toString())
        }
    }

    fun getItems(): List<DelayedTrackingItem> {
        return preferences.all.mapNotNull {
            DelayedTrackingItem(
                trackId = it.key.toLong(),
                lastEpisodeRead = it.value.toString().toFloat(),
            )
        }
    }

    data class DelayedTrackingItem(
        val trackId: Long,
        val lastEpisodeRead: Float,
    )
}
