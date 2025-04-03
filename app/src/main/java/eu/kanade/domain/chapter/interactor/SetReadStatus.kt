package eu.kanade.domain.chapter.interactor

import eu.kanade.domain.download.interactor.DeleteDownload
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.ui.library.LibraryScreenModel
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import eu.kanade.tachiyomi.ui.updates.UpdatesScreenModel
import exh.source.MERGED_SOURCE_ID
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.interactor.GetMergedChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository

class SetReadStatus(
    private val downloadPreferences: DownloadPreferences,
    private val deleteDownload: DeleteDownload,
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
    // SY -->
    private val getMergedChaptersByMangaId: GetMergedChaptersByMangaId,
    // SY <--
) {

    private val mapper = { chapter: Chapter, seen: Boolean ->
        ChapterUpdate(
            seen = seen,
            lastSecondSeen = if (!seen) 0 else null,
            id = chapter.id,
        )
    }

    /**
     * Mark chapters as seen/unseen, also delete downloaded chapters if 'After manually marked as seen' is set.
     *
     * Called from:
     *  - [LibraryScreenModel]: Manually select mangas & mark as seen
     *  - [MangaScreenModel.markChaptersRead]: Manually select chapters & mark as seen or swipe chapter as seen
     *  - [UpdatesScreenModel.markUpdatesSeen]: Manually select chapters & mark as seen
     *  - [LibraryUpdateJob.updateEpisodeList]: when a manga is updated and has new chapter but already seen,
     *  it will mark that new **duplicated** chapter as seen & delete downloading/downloaded -> should be treat as
     *  automatically ~ no auto delete
     *  - [ReaderViewModel.updateChapterProgress]: mark **duplicated** chapter as seen after finish watching -> should be
     *  treated as not manually mark as seen so not auto-delete (there are cases where chapter number is mistaken by volume number)
     */
    suspend fun await(
        seen: Boolean,
        vararg chapters: Chapter,
        // KMK -->
        manually: Boolean = true,
        // KMK <--
    ): Result = withNonCancellableContext {
        val chaptersToUpdate = chapters.filter {
            when (seen) {
                true -> !it.seen
                false -> it.seen || it.lastSecondSeen > 0
            }
        }
        if (chaptersToUpdate.isEmpty()) {
            return@withNonCancellableContext Result.NoChapters
        }

        try {
            chapterRepository.updateAll(
                chaptersToUpdate.map { mapper(it, seen) },
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
            chaptersToUpdate
                // KMK -->
                .map { it.copy(seen = true) } // mark as seen so it will respect category exclusion
                // KMK <--
                .groupBy { it.animeId }
                .forEach { (mangaId, chapters) ->
                    deleteDownload.awaitAll(
                        manga = mangaRepository.getMangaById(mangaId),
                        chapters = chapters.toTypedArray(),
                    )
                }
        }

        Result.Success
    }

    suspend fun await(mangaId: Long, seen: Boolean): Result = withNonCancellableContext {
        await(
            seen = seen,
            chapters = chapterRepository
                .getChapterByMangaId(mangaId)
                .toTypedArray(),
        )
    }

    // SY -->
    private suspend fun awaitMerged(mangaId: Long, seen: Boolean) = withNonCancellableContext f@{
        return@f await(
            seen = seen,
            chapters = getMergedChaptersByMangaId
                .await(mangaId, dedupe = false)
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
        data object NoChapters : Result
        data class InternalError(val error: Throwable) : Result
    }
}
