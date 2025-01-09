package tachiyomi.domain.updates.model

import tachiyomi.domain.anime.interactor.GetCustomMangaInfo
import tachiyomi.domain.anime.model.MangaCover
import uy.kohesive.injekt.injectLazy

data class UpdatesWithRelations(
    val animeId: Long,
    // SY -->
    val ogMangaTitle: String,
    // SY <--
    val episodeId: Long,
    val chapterName: String,
    val scanlator: String?,
    val read: Boolean,
    val bookmark: Boolean,
    val lastPageRead: Long,
    val sourceId: Long,
    val dateFetch: Long,
    val coverData: MangaCover,
) {
    // SY -->
    val mangaTitle: String = getCustomMangaInfo.get(animeId)?.title ?: ogMangaTitle

    companion object {
        private val getCustomMangaInfo: GetCustomMangaInfo by injectLazy()
    }
    // SY <--
}
