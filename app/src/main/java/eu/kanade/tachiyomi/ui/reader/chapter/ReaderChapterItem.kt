package eu.kanade.tachiyomi.ui.reader.chapter

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.chapter.model.Episode
import java.time.format.DateTimeFormatter

data class ReaderChapterItem(
    val episode: Episode,
    val manga: Manga,
    val isCurrent: Boolean,
    val dateFormat: DateTimeFormatter,
)
