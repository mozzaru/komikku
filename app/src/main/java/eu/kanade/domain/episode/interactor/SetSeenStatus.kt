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
import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.anime.repository.AnimeRepository
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.episode.interactor.GetMergedEpisodesByAnimeId
import tachiyomi.domain.episode.model.Chapter
import tachiyomi.domain.episode.model.EpisodeUpdate
import tachiyomi.domain.episode.repository.EpisodeRepository

class SetSeenStatus(
    private val downloadPreferences: DownloadPreferences,
    private val deleteDownload: DeleteDownload,
    private val animeRepository: AnimeRepository,
    private val episodeRepository: EpisodeRepository,
    // SY -->
    private val getMergedEpisodesByAnimeId: GetMergedEpisodesByAnimeId,
    // SY <--
) {

    private val mapper = { chapter: Chapter, read: Boolean ->
        EpisodeUpdate(
            read = read,
            lastPageRead = if (!read) 0 else null,
            id = chapter.id,
        )
    }

    /**
     * Mark chapters as read/unread, also delete downloaded chapters if 'After manually marked as read' is set.
     *
     * Called from:
     *  - [LibraryScreenModel]: Manually select mangas & mark as read
     *  - [AnimeScreenModel.markChaptersRead]: Manually select chapters & mark as read or swipe episode as read
     *  - [UpdatesScreenModel.markUpdatesRead]: Manually select chapters & mark as read
     *  - [LibraryUpdateJob.updateChapterList]: when a manga is updated and has new episode but already read,
     *  it will mark that new **duplicated** episode as read & delete downloading/downloaded -> should be treat as
     *  automatically ~ no auto delete
     *  - [ReaderViewModel.updateChapterProgress]: mark **duplicated** episode as read after finish reading -> should be
     *  treated as not manually mark as read so not auto-delete (there are cases where episode number is mistaken by volume number)
     */
    suspend fun await(
        read: Boolean,
        vararg chapters: Chapter,
        // KMK -->
        manually: Boolean = true,
        // KMK <--
    ): Result = withNonCancellableContext {
        val chaptersToUpdate = chapters.filter {
            when (read) {
                true -> !it.read
                false -> it.read || it.lastPageRead > 0
            }
        }
        if (chaptersToUpdate.isEmpty()) {
            return@withNonCancellableContext Result.NoChapters
        }

        try {
            episodeRepository.updateAll(
                chaptersToUpdate.map { mapper(it, read) },
            )
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            return@withNonCancellableContext Result.InternalError(e)
        }

        if (
            // KMK -->
            manually &&
            // KMK <--
            read &&
            downloadPreferences.removeAfterMarkedAsRead().get()
        ) {
            chaptersToUpdate
                // KMK -->
                .map { it.copy(read = true) } // mark as read so it will respect category exclusion
                // KMK <--
                .groupBy { it.mangaId }
                .forEach { (mangaId, chapters) ->
                    deleteDownload.awaitAll(
                        manga = animeRepository.getMangaById(mangaId),
                        chapters = chapters.toTypedArray(),
                    )
                }
        }

        Result.Success
    }

    suspend fun await(mangaId: Long, read: Boolean): Result = withNonCancellableContext {
        await(
            read = read,
            chapters = episodeRepository
                .getChapterByMangaId(mangaId)
                .toTypedArray(),
        )
    }

    // SY -->
    private suspend fun awaitMerged(mangaId: Long, read: Boolean) = withNonCancellableContext f@{
        return@f await(
            read = read,
            chapters = getMergedEpisodesByAnimeId
                .await(mangaId, dedupe = false)
                .toTypedArray(),
        )
    }

    suspend fun await(manga: Manga, read: Boolean) = if (manga.source == MERGED_SOURCE_ID) {
        awaitMerged(manga.id, read)
    } else {
        await(manga.id, read)
    }
    // SY <--

    sealed interface Result {
        data object Success : Result
        data object NoChapters : Result
        data class InternalError(val error: Throwable) : Result
    }
}
