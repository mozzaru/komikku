package eu.kanade.domain.episode.model

import eu.kanade.tachiyomi.data.database.models.EpisodeImpl
import eu.kanade.tachiyomi.source.model.SChapter
import tachiyomi.domain.chapter.model.Episode
import eu.kanade.tachiyomi.data.database.models.Episode as DbEpisode

// TODO: Remove when all deps are migrated
fun Episode.toSEpisode(): SChapter {
    return SChapter.create().also {
        it.url = url
        it.name = name
        it.date_upload = dateUpload
        it.chapter_number = episodeNumber.toFloat()
        it.scanlator = scanlator
    }
}

fun Episode.copyFromSEpisode(sChapter: SChapter): Episode {
    return this.copy(
        name = sChapter.name,
        url = sChapter.url,
        dateUpload = sChapter.date_upload,
        episodeNumber = sChapter.chapter_number.toDouble(),
        scanlator = sChapter.scanlator?.ifBlank { null }?.trim(),
    )
}

fun Episode.toDbEpisode(): DbEpisode = EpisodeImpl().also {
    it.id = id
    it.anime_id = animeId
    it.url = url
    it.name = name
    it.scanlator = scanlator
    it.seen = seen
    it.bookmark = bookmark
    it.last_second_seen = lastSecondSeen
    it.date_fetch = dateFetch
    it.date_upload = dateUpload
    it.chapter_number = episodeNumber.toFloat()
    it.source_order = sourceOrder.toInt()
    it.last_modified = lastModifiedAt
}
