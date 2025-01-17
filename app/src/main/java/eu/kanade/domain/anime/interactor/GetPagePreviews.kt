package eu.kanade.domain.anime.interactor

import eu.kanade.domain.anime.model.PagePreview
import eu.kanade.domain.anime.model.toSManga
import eu.kanade.domain.episode.model.toSChapter
import eu.kanade.tachiyomi.data.cache.PagePreviewCache
import eu.kanade.tachiyomi.animesource.ThumbnailPreviewSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import exh.source.getMainSource
import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.episode.interactor.GetEpisodesByAnimeId

class GetPagePreviews(
    private val pagePreviewCache: PagePreviewCache,
    private val getEpisodesByAnimeId: GetEpisodesByAnimeId,
) {

    suspend fun await(manga: Manga, source: AnimeSource, page: Int): Result {
        @Suppress("NAME_SHADOWING")
        val source = source.getMainSource<ThumbnailPreviewSource>() ?: return Result.Unused
        val chapters = getEpisodesByAnimeId.await(manga.id).sortedByDescending { it.sourceOrder }
        val chapterIds = chapters.map { it.id }
        return try {
            val pagePreviews = try {
                pagePreviewCache.getPageListFromCache(manga, chapterIds, page)
            } catch (e: Exception) {
                source.getThumbnailPreviewList(manga.toSManga(), chapters.map { it.toSChapter() }, page).also {
                    pagePreviewCache.putPageListToCache(manga, chapterIds, it)
                }
            }
            Result.Success(
                pagePreviews.thumbnailPreviews.map {
                    PagePreview(it.index, it.imageUrl, source.id)
                },
                pagePreviews.hasNextPage,
                pagePreviews.pagePreviewPages,
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    sealed class Result {
        data object Unused : Result()
        data class Success(
            val pagePreviews: List<PagePreview>,
            val hasNextPage: Boolean,
            val pageCount: Int?,
        ) : Result()
        data class Error(val error: Throwable) : Result()
    }
}
