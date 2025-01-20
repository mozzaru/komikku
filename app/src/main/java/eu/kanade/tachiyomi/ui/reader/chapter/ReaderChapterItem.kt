package eu.kanade.tachiyomi.ui.reader.chapter

import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.episode.model.Episode
import java.time.format.DateTimeFormatter

data class ReaderChapterItem(
    val episode: Episode,
    val manga: Anime,
    val isCurrent: Boolean,
    val dateFormat: DateTimeFormatter,
)
