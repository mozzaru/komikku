package eu.kanade.domain.episode.interactor

import eu.kanade.domain.download.interactor.DeleteDownload
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.ui.anime.AnimeScreenModel
import eu.kanade.tachiyomi.ui.library.LibraryScreenModel
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import eu.kanade.tachiyomi.ui.updates.UpdatesScreenModel
import exh.source.MERGED_SOURCE_ID
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.AnimeRepository
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.chapter.interactor.GetMergedEpisodesByAnimeId
import tachiyomi.domain.chapter.model.Episode
import tachiyomi.domain.chapter.model.EpisodeUpdate
import tachiyomi.domain.chapter.repository.EpisodeRepository

class SetSeenStatus(
    private val downloadPreferences: DownloadPreferences,
    private val deleteDownload: DeleteDownload,
    private val animeRepository: AnimeRepository,
    private val episodeRepository: EpisodeRepository,
    // SY -->
    private val getMergedEpisodesByAnimeId: GetMergedEpisodesByAnimeId,
    // SY <--
) {

    private val mapper = { episode: Episode, seen: Boolean ->
        EpisodeUpdate(
            seen = seen,
            lastSecondSeen = if (!seen) 0 else null,
            id = episode.id,
        )
    }

    /**
     * Mark episodes as seen/unseen, also delete downloaded episodes if 'After manually marked as seen' is set.
     *
     * Called from:
     *  - [LibraryScreenModel]: Manually select animes & mark as seen
     *  - [AnimeScreenModel.markEpisodesSeen]: Manually select episodes & mark as seen or swipe episode as seen
     *  - [UpdatesScreenModel.markUpdatesSeen]: Manually select episodes & mark as seen
     *  - [LibraryUpdateJob.updateEpisodeList]: when a anime is updated and has new episode but already seen,
     *  it will mark that new **duplicated** episode as seen & delete downloading/downloaded -> should be treat as
     *  automatically ~ no auto delete
     *  - [ReaderViewModel.updateChapterProgress]: mark **duplicated** episode as seen after finish watching -> should be
     *  treated as not manually mark as seen so not auto-delete (there are cases where episode number is mistaken by volume number)
     */
    suspend fun await(
        seen: Boolean,
        vararg episodes: Episode,
        // KMK -->
        manually: Boolean = true,
        // KMK <--
    ): Result = withNonCancellableContext {
        val episodesToUpdate = episodes.filter {
            when (seen) {
                true -> !it.seen
                false -> it.seen || it.lastSecondSeen > 0
            }
        }
        if (episodesToUpdate.isEmpty()) {
            return@withNonCancellableContext Result.NoEpisodes
        }

        try {
            episodeRepository.updateAll(
                episodesToUpdate.map { mapper(it, seen) },
            )
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            return@withNonCancellableContext Result.InternalError(e)
        }

        if (
            // KMK -->
            manually &&
            // KMK <--
            seen &&
            downloadPreferences.removeAfterMarkedAsSeen().get()
        ) {
            episodesToUpdate
                // KMK -->
                .map { it.copy(seen = true) } // mark as seen so it will respect category exclusion
                // KMK <--
                .groupBy { it.animeId }
                .forEach { (animeId, episodes) ->
                    deleteDownload.awaitAll(
                        manga = animeRepository.getAnimeById(animeId),
                        episodes = episodes.toTypedArray(),
                    )
                }
        }

        Result.Success
    }

    suspend fun await(animeId: Long, seen: Boolean): Result = withNonCancellableContext {
        await(
            seen = seen,
            episodes = episodeRepository
                .getEpisodeByAnimeId(animeId)
                .toTypedArray(),
        )
    }

    // SY -->
    private suspend fun awaitMerged(animeId: Long, seen: Boolean) = withNonCancellableContext f@{
        return@f await(
            seen = seen,
            episodes = getMergedEpisodesByAnimeId
                .await(animeId, dedupe = false)
                .toTypedArray(),
        )
    }

    suspend fun await(manga: Manga, seen: Boolean) = if (manga.source == MERGED_SOURCE_ID) {
        awaitMerged(manga.id, seen)
    } else {
        await(manga.id, seen)
    }
    // SY <--

    sealed interface Result {
        data object Success : Result
        data object NoEpisodes : Result
        data class InternalError(val error: Throwable) : Result
    }
}
