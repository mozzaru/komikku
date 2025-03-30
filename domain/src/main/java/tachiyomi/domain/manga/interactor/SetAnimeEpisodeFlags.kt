package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.AnimeRepository

class SetAnimeEpisodeFlags(
    private val animeRepository: AnimeRepository,
) {

    suspend fun awaitSetDownloadedFilter(manga: Manga, flag: Long): Boolean {
        return animeRepository.update(
            MangaUpdate(
                id = manga.id,
                chapterFlags = manga.chapterFlags.setFlag(flag, Manga.EPISODE_DOWNLOADED_MASK),
            ),
        )
    }

    suspend fun awaitSetUnreadFilter(manga: Manga, flag: Long): Boolean {
        return animeRepository.update(
            MangaUpdate(
                id = manga.id,
                chapterFlags = manga.chapterFlags.setFlag(flag, Manga.EPISODE_UNSEEN_MASK),
            ),
        )
    }

    suspend fun awaitSetBookmarkFilter(manga: Manga, flag: Long): Boolean {
        return animeRepository.update(
            MangaUpdate(
                id = manga.id,
                chapterFlags = manga.chapterFlags.setFlag(flag, Manga.EPISODE_BOOKMARKED_MASK),
            ),
        )
    }

    suspend fun awaitSetDisplayMode(manga: Manga, flag: Long): Boolean {
        return animeRepository.update(
            MangaUpdate(
                id = manga.id,
                chapterFlags = manga.chapterFlags.setFlag(flag, Manga.EPISODE_DISPLAY_MASK),
            ),
        )
    }

    suspend fun awaitSetSortingModeOrFlipOrder(manga: Manga, flag: Long): Boolean {
        val newFlags = manga.chapterFlags.let {
            if (manga.sorting == flag) {
                // Just flip the order
                val orderFlag = if (manga.sortDescending()) {
                    Manga.EPISODE_SORT_ASC
                } else {
                    Manga.EPISODE_SORT_DESC
                }
                it.setFlag(orderFlag, Manga.EPISODE_SORT_DIR_MASK)
            } else {
                // Set new flag with ascending order
                it
                    .setFlag(flag, Manga.EPISODE_SORTING_MASK)
                    .setFlag(Manga.EPISODE_SORT_ASC, Manga.EPISODE_SORT_DIR_MASK)
            }
        }
        return animeRepository.update(
            MangaUpdate(
                id = manga.id,
                chapterFlags = newFlags,
            ),
        )
    }

    suspend fun awaitSetAllFlags(
        animeId: Long,
        unseenFilter: Long,
        downloadedFilter: Long,
        bookmarkedFilter: Long,
        sortingMode: Long,
        sortingDirection: Long,
        displayMode: Long,
    ): Boolean {
        return animeRepository.update(
            MangaUpdate(
                id = animeId,
                chapterFlags = 0L.setFlag(unseenFilter, Manga.EPISODE_UNSEEN_MASK)
                    .setFlag(downloadedFilter, Manga.EPISODE_DOWNLOADED_MASK)
                    .setFlag(bookmarkedFilter, Manga.EPISODE_BOOKMARKED_MASK)
                    .setFlag(sortingMode, Manga.EPISODE_SORTING_MASK)
                    .setFlag(sortingDirection, Manga.EPISODE_SORT_DIR_MASK)
                    .setFlag(displayMode, Manga.EPISODE_DISPLAY_MASK),
            ),
        )
    }

    private fun Long.setFlag(flag: Long, mask: Long): Long {
        return this and mask.inv() or (flag and mask)
    }
}
