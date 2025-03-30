package tachiyomi.domain.episode.interactor

import exh.source.MERGED_SOURCE_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.anime.interactor.GetMergedReferencesById
import tachiyomi.domain.anime.model.MergedAnimeReference
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.episode.repository.EpisodeRepository

class GetMergedEpisodesByAnimeId(
    private val episodeRepository: EpisodeRepository,
    private val getMergedReferencesById: GetMergedReferencesById,
) {

    suspend fun await(
        animeId: Long,
        dedupe: Boolean = true,
        applyScanlatorFilter: Boolean = false,
    ): List<Episode> {
        return transformMergedEpisodes(
            getMergedReferencesById.await(animeId),
            getFromDatabase(animeId, applyScanlatorFilter),
            dedupe,
        )
    }

    suspend fun subscribe(
        aniemId: Long,
        dedupe: Boolean = true,
        applyScanlatorFilter: Boolean = false,
    ): Flow<List<Episode>> {
        return try {
            episodeRepository.getMergedEpisodeByAnimeIdAsFlow(aniemId, applyScanlatorFilter)
                .combine(getMergedReferencesById.subscribe(aniemId)) { episodes, references ->
                    transformMergedEpisodes(references, episodes, dedupe)
                }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            flowOf(emptyList())
        }
    }

    private suspend fun getFromDatabase(
        animeId: Long,
        applyScanlatorFilter: Boolean = false,
    ): List<Episode> {
        return try {
            episodeRepository.getMergedEpisodeByAnimeId(animeId, applyScanlatorFilter)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    private fun transformMergedEpisodes(
        animeReferences: List<MergedAnimeReference>,
        episodeList: List<Episode>,
        dedupe: Boolean,
    ): List<Episode> {
        return if (dedupe) dedupeEpisodeList(animeReferences, episodeList) else episodeList
    }

    private fun dedupeEpisodeList(
        animeReferences: List<MergedAnimeReference>,
        episodeList: List<Episode>,
    ): List<Episode> {
        return when (animeReferences.firstOrNull { it.animeSourceId == MERGED_SOURCE_ID }?.episodeSortMode) {
            MergedAnimeReference.EPISODE_SORT_NO_DEDUPE, MergedAnimeReference.EPISODE_SORT_NONE -> episodeList
            MergedAnimeReference.EPISODE_SORT_PRIORITY -> dedupeByPriority(animeReferences, episodeList)
            MergedAnimeReference.EPISODE_SORT_MOST_EPISODES -> {
                findSourceWithMostEpisodes(episodeList)?.let { animeId ->
                    episodeList.filter { it.animeId == animeId }
                } ?: episodeList
            }
            MergedAnimeReference.EPISODE_SORT_HIGHEST_EPISODE_NUMBER -> {
                findSourceWithHighestEpisodeNumber(episodeList)?.let { animeId ->
                    episodeList.filter { it.animeId == animeId }
                } ?: episodeList
            }
            else -> episodeList
        }
    }

    private fun findSourceWithMostEpisodes(episodeList: List<Episode>): Long? {
        return episodeList.groupBy { it.animeId }.maxByOrNull { it.value.size }?.key
    }

    private fun findSourceWithHighestEpisodeNumber(episodeList: List<Episode>): Long? {
        return episodeList.maxByOrNull { it.episodeNumber }?.animeId
    }

    private fun dedupeByPriority(
        animeReferences: List<MergedAnimeReference>,
        episodeList: List<Episode>,
    ): List<Episode> {
        val sortedEpisodeList = mutableListOf<Episode>()

        var existingEpisodeIndex: Int
        episodeList.groupBy { it.animeId }
            .entries
            .sortedBy { (animeId) ->
                animeReferences.find { it.animeId == animeId }?.episodePriority ?: Int.MAX_VALUE
            }
            .forEach { (_, episodes) ->
                existingEpisodeIndex = -1
                episodes.forEach { episode ->
                    val oldEpisodeIndex = existingEpisodeIndex
                    if (episode.isRecognizedNumber) {
                        existingEpisodeIndex = sortedEpisodeList.indexOfFirst {
                            // check if the episode is not already there
                            it.isRecognizedNumber &&
                                it.episodeNumber == episode.episodeNumber &&
                                // allow multiple episodes of the same number from the same source
                                it.animeId != episode.animeId
                        }
                        if (existingEpisodeIndex == -1) {
                            sortedEpisodeList.add(oldEpisodeIndex + 1, episode)
                            existingEpisodeIndex = oldEpisodeIndex + 1
                        }
                    } else {
                        sortedEpisodeList.add(oldEpisodeIndex + 1, episode)
                        existingEpisodeIndex = oldEpisodeIndex + 1
                    }
                }
            }

        return sortedEpisodeList.mapIndexed { index, episode ->
            episode.copy(sourceOrder = index.toLong())
        }
    }
}
