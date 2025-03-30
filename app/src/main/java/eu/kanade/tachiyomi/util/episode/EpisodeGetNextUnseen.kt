package eu.kanade.tachiyomi.util.episode

import eu.kanade.domain.episode.model.applyFilters
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.anime.EpisodeList
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.chapter.model.Episode

/**
 * Gets next unseen episode with filters and sorting applied
 */
fun List<Episode>.getNextUnseen(
    manga: Manga,
    downloadManager: DownloadManager /* SY --> */,
    mergedManga: Map<Long, Manga>, /* SY <-- */
): Episode? {
    return applyFilters(manga, downloadManager/* SY --> */, mergedManga/* SY <-- */).let { chapters ->
        if (manga.sortDescending()) {
            chapters.findLast { !it.seen }
        } else {
            chapters.find { !it.seen }
        }
    }
}

/**
 * Gets next unseen episode with filters and sorting applied
 */
fun List<EpisodeList.Item>.getNextUnseen(manga: Manga): Episode? {
    return applyFilters(manga).let { chapters ->
        if (manga.sortDescending()) {
            chapters.findLast { !it.episode.seen }
        } else {
            chapters.find { !it.episode.seen }
        }
    }?.episode
}
