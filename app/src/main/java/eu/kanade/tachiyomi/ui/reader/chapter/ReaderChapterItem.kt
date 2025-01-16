package eu.kanade.tachiyomi.ui.reader.chapter

import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.episode.model.Episode
import java.time.format.DateTimeFormatter

data class ReaderChapterItem(
    val episode: Episode,
    val manga: Manga,
    val isCurrent: Boolean,
    val dateFormat: DateTimeFormatter,
)
