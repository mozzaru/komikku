@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.source.model.SChapter
import java.io.Serializable
import tachiyomi.domain.episode.model.Chapter as DomainChapter

interface Episode : SChapter, Serializable {

    var id: Long?

    var manga_id: Long?

    var read: Boolean

    var bookmark: Boolean

    var last_page_read: Int

    var date_fetch: Long

    var source_order: Int

    var last_modified: Long

    var version: Long
}

fun Episode.toDomainChapter(): DomainChapter? {
    if (id == null || manga_id == null) return null
    return DomainChapter(
        id = id!!,
        mangaId = manga_id!!,
        read = read,
        bookmark = bookmark,
        lastPageRead = last_page_read.toLong(),
        dateFetch = date_fetch,
        sourceOrder = source_order.toLong(),
        url = url,
        name = name,
        dateUpload = date_upload,
        chapterNumber = chapter_number.toDouble(),
        scanlator = scanlator,
        lastModifiedAt = last_modified,
        version = version,
    )
}
