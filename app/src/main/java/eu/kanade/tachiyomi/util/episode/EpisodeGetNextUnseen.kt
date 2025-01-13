package eu.kanade.tachiyomi.util.episode

import eu.kanade.domain.episode.model.applyFilters
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.anime.EpisodeList
import exh.source.isEhBasedManga
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.episode.model.Episode

/**
 * Gets next unread episode with filters and sorting applied
 */
fun List<Episode>.getNextUnread(
    anime: Anime,
    downloadManager: DownloadManager /* SY --> */,
    mergedAnime: Map<Long, Anime>, /* SY <-- */
): Episode? {
    return applyFilters(anime, downloadManager/* SY --> */, mergedAnime/* SY <-- */).let { episodes ->
        // SY -->
        if (anime.isEhBasedManga()) {
            return@let if (anime.sortDescending()) {
                episodes.firstOrNull()?.takeUnless { it.seen }
            } else {
                episodes.lastOrNull()?.takeUnless { it.seen }
            }
        }
        // SY <--
        if (anime.sortDescending()) {
            episodes.findLast { !it.seen }
        } else {
            episodes.find { !it.seen }
        }
    }
}

/**
 * Gets next unread episode with filters and sorting applied
 */
fun List<EpisodeList.Item>.getNextUnread(anime: Anime): Episode? {
    return applyFilters(anime).let { episodes ->
        // SY -->
        if (anime.isEhBasedManga()) {
            return@let if (anime.sortDescending()) {
                episodes.firstOrNull()?.takeUnless { it.episode.seen }
            } else {
                episodes.lastOrNull()?.takeUnless { it.episode.seen }
            }
        }
        // SY <--
        if (anime.sortDescending()) {
            episodes.findLast { !it.episode.seen }
        } else {
            episodes.find { !it.episode.seen }
        }
    }?.episode
}
