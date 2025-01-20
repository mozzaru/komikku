package eu.kanade.domain.episode.model

import eu.kanade.tachiyomi.data.database.models.EpisodeImpl
import eu.kanade.tachiyomi.source.model.SEpisode
import tachiyomi.domain.episode.model.Episode
import eu.kanade.tachiyomi.data.database.models.Episode as DbChapter

// TODO: Remove when all deps are migrated
fun Episode.toSChapter(): SEpisode {
    return SEpisode.create().also {
        it.url = url
        it.name = name
        it.date_upload = dateUpload
        it.chapter_number = chapterNumber.toFloat()
        it.scanlator = scanlator
    }
}

fun Episode.copyFromSChapter(sEpisode: SEpisode): Episode {
    return this.copy(
        name = sEpisode.name,
        url = sEpisode.url,
        dateUpload = sEpisode.date_upload,
        chapterNumber = sEpisode.chapter_number.toDouble(),
        scanlator = sEpisode.scanlator?.ifBlank { null }?.trim(),
    )
}

fun Episode.toDbChapter(): DbChapter = EpisodeImpl().also {
    it.id = id
    it.manga_id = mangaId
    it.url = url
    it.name = name
    it.scanlator = scanlator
    it.read = read
    it.bookmark = bookmark
    it.last_page_read = lastPageRead.toInt()
    it.date_fetch = dateFetch
    it.date_upload = dateUpload
    it.chapter_number = chapterNumber.toFloat()
    it.source_order = sourceOrder.toInt()
    it.last_modified = lastModifiedAt
}
