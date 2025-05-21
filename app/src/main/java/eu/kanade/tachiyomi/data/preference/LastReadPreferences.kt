package eu.kanade.tachiyomi.data.preference

import android.content.Context
import android.content.SharedPreferences

class LastReadPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("last_read", Context.MODE_PRIVATE)

    fun saveLastRead(mangaId: Long, chapterId: Long, page: Int) {
        prefs.edit()
            .putLong("last_manga", mangaId)
            .putLong("last_chapter", chapterId)
            .putInt("last_page", page)
            .apply()
    }

    fun getLastRead(): LastReadState? {
        val mangaId = prefs.getLong("last_manga", -1L)
        val chapterId = prefs.getLong("last_chapter", -1L)
        val page = prefs.getInt("last_page", -1)
        return if (mangaId != -1L && chapterId != -1L && page != -1)
            LastReadState(mangaId, chapterId, page)
        else null
    }

    data class LastReadState(val mangaId: Long, val chapterId: Long, val page: Int)
}