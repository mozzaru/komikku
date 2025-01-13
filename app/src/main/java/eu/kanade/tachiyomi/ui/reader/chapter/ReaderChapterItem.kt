package eu.kanade.tachiyomi.ui.reader.episode

import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.episode.model.Episode
import java.time.format.DateTimeFormatter

data class ReaderEpisodeItem(
    val episode: Episode,
    val anime: Anime,
    val isCurrent: Boolean,
    val dateFormat: DateTimeFormatter,
)
