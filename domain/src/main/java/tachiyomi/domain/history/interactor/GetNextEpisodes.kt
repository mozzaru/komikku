package tachiyomi.domain.history.interactor

import exh.source.MERGED_SOURCE_ID
import exh.source.isEhBasedManga
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.episode.interactor.GetMergedEpisodesByAnimeId
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.episode.service.getEpisodeSort
import tachiyomi.domain.history.repository.HistoryRepository
import kotlin.math.max

class GetNextEpisodes(
    private val getEpisodesByAnimeId: GetEpisodesByAnimeId,
    // SY -->
    private val getMergedEpisodesByAnimeId: GetMergedEpisodesByAnimeId,
    // SY <--
    private val getAnime: GetAnime,
    private val historyRepository: HistoryRepository,
) {

    suspend fun await(onlyUnread: Boolean = true): List<Episode> {
        val history = historyRepository.getLastHistory() ?: return emptyList()
        return await(history.animeId, history.episodeId, onlyUnread)
    }

    suspend fun await(animeId: Long, onlyUnread: Boolean = true): List<Episode> {
        val anime = getAnime.await(animeId) ?: return emptyList()

        // SY -->
        if (anime.source == MERGED_SOURCE_ID) {
            val episodes = getMergedEpisodesByAnimeId.await(animeId, applyScanlatorFilter = true)
                .sortedWith(getEpisodeSort(anime, sortDescending = false))

            return if (onlyUnread) {
                episodes.filterNot { it.seen }
            } else {
                episodes
            }
        }
        if (anime.isEhBasedManga()) {
            val episodes = getEpisodesByAnimeId.await(animeId, applyScanlatorFilter = true)
                .sortedWith(getEpisodeSort(anime, sortDescending = false))

            return if (onlyUnread) {
                episodes.takeLast(1).takeUnless { it.firstOrNull()?.seen == true }.orEmpty()
            } else {
                episodes
            }
        }
        // SY <--

        val episodes = getEpisodesByAnimeId.await(animeId, applyScanlatorFilter = true)
            .sortedWith(getEpisodeSort(anime, sortDescending = false))

        return if (onlyUnread) {
            episodes.filterNot { it.seen }
        } else {
            episodes
        }
    }

    suspend fun await(
        animeId: Long,
        fromEpisodeId: Long,
        onlyUnread: Boolean = true,
    ): List<Episode> {
        val episodes = await(animeId, onlyUnread)
        val currEpisodeIndex = episodes.indexOfFirst { it.id == fromEpisodeId }
        val nextEpisodes = episodes.subList(max(0, currEpisodeIndex), episodes.size)

        if (onlyUnread) {
            return nextEpisodes
        }

        // The "next episode" is either:
        // - The current episode if it isn't completely read
        // - The episodes after the current episode if the current one is completely read
        val fromEpisode = episodes.getOrNull(currEpisodeIndex)
        return if (fromEpisode != null && !fromEpisode.seen) {
            nextEpisodes
        } else {
            nextEpisodes.drop(1)
        }
    }
}
