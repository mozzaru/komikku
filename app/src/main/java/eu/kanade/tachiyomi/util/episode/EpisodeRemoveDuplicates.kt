package eu.kanade.tachiyomi.util.episode

import tachiyomi.domain.chapter.model.Episode

/**
 * Returns a copy of the list with duplicate episodes removed
 */
fun List<Episode>.removeDuplicates(currentEpisode: Episode): List<Episode> {
    return groupBy { it.episodeNumber }
        .map { (_, chapters) ->
            chapters.find { it.id == currentEpisode.id }
                ?: chapters.find { it.scanlator == currentEpisode.scanlator }
                ?: chapters.first()
        }
}
