package eu.kanade.tachiyomi.util.episode

import eu.kanade.tachiyomi.data.download.DownloadCache
import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.episode.model.Episode
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Returns a copy of the list with not downloaded episodes removed.
 */
fun List<Episode>.filterDownloaded(manga: Manga/* SY --> */, mangaMap: Map<Long, Manga>?): List<Episode> {
    if (manga.isLocal()) return this

    val downloadCache: DownloadCache = Injekt.get()

    // SY -->
    return filter {
        val chapterManga = mangaMap?.get(it.mangaId) ?: manga
        downloadCache.isChapterDownloaded(it.name, it.scanlator, chapterManga.ogTitle, chapterManga.source, false)
    }
    // SY <--
}
