package eu.kanade.domain.chapter.model

import eu.kanade.domain.manga.model.downloadedFilter
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.anime.EpisodeList
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.service.getChapterSort
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.applyFilter
import tachiyomi.source.local.isLocal

/**
 * Applies the view filters to the list of chapters obtained from the database.
 * @return an observable of the list of chapters filtered and sorted.
 */
fun List<Chapter>.applyFilters(
    manga: Manga,
    downloadManager: DownloadManager, /* SY --> */
    mergedManga: Map<Long, Manga>, /* SY <-- */
): List<Chapter> {
    val isLocalManga = manga.isLocal()
    val unseenFilter = manga.unseenFilter
    val downloadedFilter = manga.downloadedFilter
    val bookmarkedFilter = manga.bookmarkedFilter

    return filter { episode -> applyFilter(unseenFilter) { !episode.seen } }
        .filter { episode -> applyFilter(bookmarkedFilter) { episode.bookmark } }
        .filter { episode ->
            // SY -->
            @Suppress("NAME_SHADOWING")
            val anime = mergedManga.getOrElse(episode.animeId) { manga }
            // SY <--
            applyFilter(downloadedFilter) {
                val downloaded = downloadManager.isEpisodeDownloaded(
                    episode.name,
                    episode.scanlator,
                    /* SY --> */ anime.ogTitle /* SY <-- */,
                    anime.source,
                )
                downloaded || isLocalManga
            }
        }
        .sortedWith(getChapterSort(manga))
}

/**
 * Applies the view filters to the list of chapters obtained from the database.
 * @return an observable of the list of chapters filtered and sorted.
 */
fun List<EpisodeList.Item>.applyFilters(manga: Manga): Sequence<EpisodeList.Item> {
    val isLocalAnime = manga.isLocal()
    val unseenFilter = manga.unseenFilter
    val downloadedFilter = manga.downloadedFilter
    val bookmarkedFilter = manga.bookmarkedFilter
    return asSequence()
        .filter { (episode) -> applyFilter(unseenFilter) { !episode.seen } }
        .filter { (episode) -> applyFilter(bookmarkedFilter) { episode.bookmark } }
        .filter { applyFilter(downloadedFilter) { it.isDownloaded || isLocalAnime } }
        .sortedWith { (episode1), (episode2) -> getChapterSort(manga).invoke(episode1, episode2) }
}
