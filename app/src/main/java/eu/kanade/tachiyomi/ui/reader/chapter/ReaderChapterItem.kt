package eu.kanade.tachiyomi.ui.reader.chapter

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.chapter.model.Chapter
import java.time.format.DateTimeFormatter

data class ReaderChapterItem(
    val chapter: Chapter,
    val manga: Manga,
    val isCurrent: Boolean,
    val dateFormat: DateTimeFormatter,
)
