package eu.kanade.tachiyomi.util.episode

import eu.kanade.domain.episode.model.applyFilters
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.anime.EpisodeList
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
        if (anime.sortDescending()) {
            chapters.findLast { !it.seen }
        } else {
            chapters.find { !it.seen }
        }
    }
}

/**
 * Gets next unseen episode with filters and sorting applied
 */
fun List<EpisodeList.Item>.getNextUnseen(anime: Anime): Episode? {
    return applyFilters(anime).let { chapters ->
        if (anime.sortDescending()) {
            chapters.findLast { !it.episode.seen }
        } else {
            chapters.find { !it.episode.seen }
        }
    }?.episode
}
