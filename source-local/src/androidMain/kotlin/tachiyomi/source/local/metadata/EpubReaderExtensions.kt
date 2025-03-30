package tachiyomi.source.local.metadata

import eu.kanade.tachiyomi.source.model.SAnime
import eu.kanade.tachiyomi.source.model.SEpisode
import mihon.core.archive.EpubReader
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Fills anime and episode metadata using this epub file's metadata.
 */
fun EpubReader.fillMetadata(anime: SAnime, episode: SEpisode) {
    val ref = getPackageHref()
    val doc = getPackageDocument(ref)

    val title = doc.getElementsByTag("dc:title").first()
    val publisher = doc.getElementsByTag("dc:publisher").first()
    val creator = doc.getElementsByTag("dc:creator").first()
    val description = doc.getElementsByTag("dc:description").first()
    var date = doc.getElementsByTag("dc:date").first()
    if (date == null) {
        date = doc.select("meta[property=dcterms:modified]").first()
    }

    creator?.text()?.let { anime.author = it }
    description?.text()?.let { anime.description = it }

    title?.text()?.let { episode.name = it }

    if (publisher != null) {
        episode.scanlator = publisher.text()
    } else if (creator != null) {
        episode.scanlator = creator.text()
    }

    if (date != null) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())
        try {
            val parsedDate = dateFormat.parse(date.text())
            if (parsedDate != null) {
                episode.date_upload = parsedDate.time
            }
        } catch (e: ParseException) {
            // Empty
        }
    }
}
