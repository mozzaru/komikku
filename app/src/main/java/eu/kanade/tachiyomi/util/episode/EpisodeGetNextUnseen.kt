package eu.kanade.tachiyomi.util.episode

import eu.kanade.domain.episode.model.applyFilters
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.anime.EpisodeList
import exh.source.isEhBasedManga
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.episode.model.Episode

/**
 * Gets next unseen episode with filters and sorting applied
 */
fun List<Episode>.getNextUnseen(
    manga: Anime,
    downloadManager: DownloadManager /* SY --> */,
    mergedManga: Map<Long, Anime>, /* SY <-- */
): Episode? {
    return applyFilters(manga, downloadManager/* SY --> */, mergedManga/* SY <-- */).let { chapters ->
        // SY -->
        if (manga.isEhBasedManga()) {
            return@let if (manga.sortDescending()) {
                chapters.firstOrNull()?.takeUnless { it.read }
            } else {
                chapters.lastOrNull()?.takeUnless { it.read }
            }
        }
        // SY <--
        if (manga.sortDescending()) {
            chapters.findLast { !it.read }
        } else {
            chapters.find { !it.read }
        }
    }
}

/**
 * Gets next unseen episode with filters and sorting applied
 */
fun List<EpisodeList.Item>.getNextUnseen(manga: Anime): Episode? {
    return applyFilters(manga).let { chapters ->
        // SY -->
        if (manga.isEhBasedManga()) {
            return@let if (manga.sortDescending()) {
                chapters.firstOrNull()?.takeUnless { it.episode.read }
            } else {
                chapters.lastOrNull()?.takeUnless { it.episode.read }
            }
        }
        // SY <--
        if (manga.sortDescending()) {
            chapters.findLast { !it.episode.read }
        } else {
            chapters.find { !it.episode.read }
        }
    }?.episode
}
