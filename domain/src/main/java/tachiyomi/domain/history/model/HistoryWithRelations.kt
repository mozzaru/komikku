package tachiyomi.domain.history.model

import tachiyomi.domain.anime.interactor.GetCustomAnimeInfo
import tachiyomi.domain.anime.model.AnimeCover
import uy.kohesive.injekt.injectLazy
import java.util.Date

data class HistoryWithRelations(
    val id: Long,
    val chapterId: Long,
    val mangaId: Long,
    // SY -->
    val ogTitle: String,
    // SY <--
    val chapterNumber: Double,
    val readAt: Date?,
    val readDuration: Long,
    val coverData: AnimeCover,
) {
    // SY -->
    val title: String = customMangaManager.get(mangaId)?.title ?: ogTitle

    companion object {
        private val customMangaManager: GetCustomAnimeInfo by injectLazy()
    }
    // SY <--
}
