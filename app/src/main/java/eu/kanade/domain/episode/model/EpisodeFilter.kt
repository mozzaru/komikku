package eu.kanade.domain.episode.model

import eu.kanade.domain.anime.model.downloadedFilter
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.anime.EpisodeList
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.applyFilter
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.episode.service.getEpisodeSort
import tachiyomi.source.local.isLocal

/**
 * Applies the view filters to the list of episodes obtained from the database.
 * @return an observable of the list of episodes filtered and sorted.
 */
fun List<Episode>.applyFilters(
    anime: Anime,
    downloadManager: DownloadManager, /* SY --> */
    mergedAnime: Map<Long, Anime>, /* SY <-- */
): List<Episode> {
    val isLocalManga = anime.isLocal()
    val unreadFilter = anime.unreadFilter
    val downloadedFilter = anime.downloadedFilter
    val bookmarkedFilter = anime.bookmarkedFilter

    return filter { chapter -> applyFilter(unreadFilter) { !chapter.read } }
        .filter { chapter -> applyFilter(bookmarkedFilter) { chapter.bookmark } }
        .filter { chapter ->
            // SY -->
            @Suppress("NAME_SHADOWING")
            val manga = mergedAnime.getOrElse(chapter.mangaId) { anime }
            // SY <--
            applyFilter(downloadedFilter) {
                val downloaded = downloadManager.isChapterDownloaded(
                    chapter.name,
                    chapter.scanlator,
                    /* SY --> */ manga.ogTitle /* SY <-- */,
                    manga.source,
                )
                downloaded || isLocalManga
            }
        }
        .sortedWith(getEpisodeSort(anime))
}

/**
 * Applies the view filters to the list of episodes obtained from the database.
 * @return an observable of the list of episodes filtered and sorted.
 */
fun List<EpisodeList.Item>.applyFilters(anime: Anime): Sequence<EpisodeList.Item> {
    val isLocalManga = anime.isLocal()
    val unreadFilter = anime.unreadFilter
    val downloadedFilter = anime.downloadedFilter
    val bookmarkedFilter = anime.bookmarkedFilter
    return asSequence()
        .filter { (chapter) -> applyFilter(unreadFilter) { !chapter.read } }
        .filter { (chapter) -> applyFilter(bookmarkedFilter) { chapter.bookmark } }
        .filter { applyFilter(downloadedFilter) { it.isDownloaded || isLocalManga } }
        .sortedWith { (chapter1), (chapter2) -> getEpisodeSort(anime).invoke(chapter1, chapter2) }
}
