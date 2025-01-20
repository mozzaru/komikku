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
    anime: Anime,
    downloadManager: DownloadManager /* SY --> */,
    mergedAnime: Map<Long, Anime>, /* SY <-- */
): Episode? {
    return applyFilters(anime, downloadManager/* SY --> */, mergedAnime/* SY <-- */).let { chapters ->
        // SY -->
        if (anime.isEhBasedManga()) {
            return@let if (anime.sortDescending()) {
                chapters.firstOrNull()?.takeUnless { it.read }
            } else {
                chapters.lastOrNull()?.takeUnless { it.read }
            }
        }
        // SY <--
        if (anime.sortDescending()) {
            chapters.findLast { !it.read }
        } else {
            chapters.find { !it.read }
        }
    }
}

/**
 * Gets next unseen episode with filters and sorting applied
 */
fun List<EpisodeList.Item>.getNextUnseen(anime: Anime): Episode? {
    return applyFilters(anime).let { chapters ->
        // SY -->
        if (anime.isEhBasedManga()) {
            return@let if (anime.sortDescending()) {
                chapters.firstOrNull()?.takeUnless { it.episode.read }
            } else {
                chapters.lastOrNull()?.takeUnless { it.episode.read }
            }
        }
        // SY <--
        if (anime.sortDescending()) {
            chapters.findLast { !it.episode.read }
        } else {
            chapters.find { !it.episode.read }
        }
    }?.episode
}
