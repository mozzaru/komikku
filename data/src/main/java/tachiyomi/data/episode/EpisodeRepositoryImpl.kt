package tachiyomi.data.episode

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.lang.toLong
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.episode.model.EpisodeUpdate
import tachiyomi.domain.episode.repository.EpisodeRepository

class EpisodeRepositoryImpl(
    private val handler: DatabaseHandler,
) : EpisodeRepository {

    override suspend fun addAll(episodes: List<Episode>): List<Episode> {
        return try {
            handler.await(inTransaction = true) {
                episodes.map { chapter ->
                    episodesQueries.insert(
                        chapter.animeId,
                        chapter.url,
                        chapter.name,
                        chapter.scanlator,
                        chapter.seen,
                        chapter.bookmark,
                        chapter.lastSecondSeen,
                        chapter.episodeNumber,
                        chapter.sourceOrder,
                        chapter.dateFetch,
                        chapter.dateUpload,
                        chapter.version,
                    )
                    val lastInsertId = episodesQueries.selectLastInsertedRowId().executeAsOne()
                    chapter.copy(id = lastInsertId)
                }
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    override suspend fun update(episodeUpdate: EpisodeUpdate) {
        partialUpdate(episodeUpdate)
    }

    override suspend fun updateAll(episodeUpdates: List<EpisodeUpdate>) {
        partialUpdate(*episodeUpdates.toTypedArray())
    }

    private suspend fun partialUpdate(vararg episodeUpdates: EpisodeUpdate) {
        handler.await(inTransaction = true) {
            episodeUpdates.forEach { chapterUpdate ->
                episodesQueries.update(
                    mangaId = chapterUpdate.mangaId,
                    url = chapterUpdate.url,
                    name = chapterUpdate.name,
                    scanlator = chapterUpdate.scanlator,
                    read = chapterUpdate.seen,
                    bookmark = chapterUpdate.bookmark,
                    lastPageRead = chapterUpdate.lastSecondSeen,
                    chapterNumber = chapterUpdate.chapterNumber,
                    sourceOrder = chapterUpdate.sourceOrder,
                    dateFetch = chapterUpdate.dateFetch,
                    dateUpload = chapterUpdate.dateUpload,
                    chapterId = chapterUpdate.id,
                    version = chapterUpdate.version,
                    isSyncing = 0,
                )
            }
        }
    }

    override suspend fun removeEpisodesWithIds(episodeIds: List<Long>) {
        try {
            handler.await { episodesQueries.removeEpisodesWithIds(episodeIds) }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    override suspend fun getEpisodeByAnimeId(animeId: Long, applyScanlatorFilter: Boolean): List<Episode> {
        return handler.awaitList {
            episodesQueries.getEpisodesByAnimeId(animeId, applyScanlatorFilter.toLong(), EpisodeMapper::mapEpisode)
        }
    }

    override suspend fun getScanlatorsByAnimeId(animeId: Long): List<String> {
        return handler.awaitList {
            episodesQueries.getScanlatorsByAnimeId(animeId) { it.orEmpty() }
        }
    }

    override fun getScanlatorsByAnimeIdAsFlow(animeId: Long): Flow<List<String>> {
        return handler.subscribeToList {
            episodesQueries.getScanlatorsByAnimeId(animeId) { it.orEmpty() }
        }
    }

    override suspend fun getBookmarkedEpisodesByAnimeId(animeId: Long): List<Episode> {
        return handler.awaitList {
            episodesQueries.getBookmarkedEpisodesByAnimeId(
                animeId,
                EpisodeMapper::mapEpisode,
            )
        }
    }

    override suspend fun getEpisodeById(id: Long): Episode? {
        return handler.awaitOneOrNull { episodesQueries.getEpisodeById(id, EpisodeMapper::mapEpisode) }
    }

    override suspend fun getEpisodeByAnimeIdAsFlow(animeId: Long, applyScanlatorFilter: Boolean): Flow<List<Episode>> {
        return handler.subscribeToList {
            episodesQueries.getEpisodesByAnimeId(animeId, applyScanlatorFilter.toLong(), EpisodeMapper::mapEpisode)
        }
    }

    override suspend fun getEpisodeByUrlAndAnimeId(url: String, animeId: Long): Episode? {
        return handler.awaitOneOrNull {
            episodesQueries.getEpisodeByUrlAndAnimeId(
                url,
                animeId,
                EpisodeMapper::mapEpisode,
            )
        }
    }

    // SY -->
    override suspend fun getEpisodeByUrl(url: String): List<Episode> {
        return handler.awaitList { episodesQueries.getEpisodeByUrl(url, EpisodeMapper::mapEpisode) }
    }

    override suspend fun getMergedEpisodeByAnimeId(animeId: Long, applyScanlatorFilter: Boolean): List<Episode> {
        return handler.awaitList {
            episodesQueries.getMergedEpisodesByAnimeId(
                animeId,
                applyScanlatorFilter.toLong(),
                EpisodeMapper::mapEpisode,
            )
        }
    }

    override suspend fun getMergedEpisodeByAnimeIdAsFlow(
        animeId: Long,
        applyScanlatorFilter: Boolean,
    ): Flow<List<Episode>> {
        return handler.subscribeToList {
            episodesQueries.getMergedEpisodesByAnimeId(
                animeId,
                applyScanlatorFilter.toLong(),
                EpisodeMapper::mapEpisode,
            )
        }
    }

    override suspend fun getScanlatorsByMergeId(animeId: Long): List<String> {
        return handler.awaitList {
            episodesQueries.getScanlatorsByMergeId(animeId) { it.orEmpty() }
        }
    }

    override fun getScanlatorsByMergeIdAsFlow(animeId: Long): Flow<List<String>> {
        return handler.subscribeToList {
            episodesQueries.getScanlatorsByMergeId(animeId) { it.orEmpty() }
        }
    }
    // SY <--
}
