package tachiyomi.data.anime

import tachiyomi.domain.anime.model.MergedAnimeReference

object MergedAnimeMapper {
    fun map(
        id: Long,
        isInfoManga: Boolean,
        getChapterUpdates: Boolean,
        chapterSortMode: Long,
        chapterPriority: Long,
        downloadChapters: Boolean,
        mergeId: Long,
        mergeUrl: String,
        mangaId: Long?,
        mangaUrl: String,
        mangaSourceId: Long,
    ): MergedAnimeReference {
        return MergedAnimeReference(
            id = id,
            isInfoAnime = isInfoManga,
            getChapterUpdates = getChapterUpdates,
            chapterSortMode = chapterSortMode.toInt(),
            chapterPriority = chapterPriority.toInt(),
            downloadChapters = downloadChapters,
            mergeId = mergeId,
            mergeUrl = mergeUrl,
            animeId = mangaId,
            animeUrl = mangaUrl,
            animeSourceId = mangaSourceId,
        )
    }
}
