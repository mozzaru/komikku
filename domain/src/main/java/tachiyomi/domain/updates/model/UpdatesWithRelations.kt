package tachiyomi.domain.updates.model

import tachiyomi.domain.anime.interactor.GetCustomAnimeInfo
import tachiyomi.domain.anime.model.AnimeCover
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
    val coverData: AnimeCover,
) {
    // SY -->
    val mangaTitle: String = getCustomAnimeInfo.get(animeId)?.title ?: ogMangaTitle

    companion object {
        private val getCustomAnimeInfo: GetCustomAnimeInfo by injectLazy()
    }
    // SY <--
}
