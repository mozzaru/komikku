package eu.kanade.domain.anime.interactor

import eu.kanade.domain.anime.model.PagePreview
import eu.kanade.domain.anime.model.toSAnime
import eu.kanade.domain.episode.model.toSEpisode
import eu.kanade.tachiyomi.data.cache.PagePreviewCache
import eu.kanade.tachiyomi.source.PagePreviewSource
import eu.kanade.tachiyomi.source.Source
import exh.source.getMainSource
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.episode.interactor.GetEpisodesByAnimeId

class GetPagePreviews(
    private val pagePreviewCache: PagePreviewCache,
    private val getEpisodesByAnimeId: GetEpisodesByAnimeId,
) {

    suspend fun await(anime: Anime, source: Source, page: Int): Result {
        @Suppress("NAME_SHADOWING")
        val source = source.getMainSource<PagePreviewSource>() ?: return Result.Unused
        val episodes = getEpisodesByAnimeId.await(anime.id).sortedByDescending { it.sourceOrder }
        val episodeIds = episodes.map { it.id }
        return try {
            val pagePreviews = try {
                pagePreviewCache.getPageListFromCache(anime, episodeIds, page)
            } catch (e: Exception) {
                source.getPagePreviewList(anime.toSAnime(), episodes.map { it.toSEpisode() }, page).also {
                    pagePreviewCache.putPageListToCache(anime, episodeIds, it)
                }
            }
            Result.Success(
                pagePreviews.pagePreviews.map {
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
