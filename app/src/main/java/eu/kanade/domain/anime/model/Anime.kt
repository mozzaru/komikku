package eu.kanade.domain.anime.model

import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import mihon.core.archive.CbzCrypto
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.metadata.comicinfo.ComicInfo
import tachiyomi.core.metadata.comicinfo.ComicInfoPublishingStatus
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.episode.model.Episode
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// TODO: move these into the domain model
val Anime.readingMode: Long
    get() = viewerFlags and ReadingMode.MASK.toLong()

val Anime.readerOrientation: Long
    get() = viewerFlags and ReaderOrientation.MASK.toLong()

val Anime.downloadedFilter: TriState
    get() {
        if (forceDownloaded()) return TriState.ENABLED_IS
        return when (downloadedFilterRaw) {
            Anime.CHAPTER_SHOW_DOWNLOADED -> TriState.ENABLED_IS
            Anime.CHAPTER_SHOW_NOT_DOWNLOADED -> TriState.ENABLED_NOT
            else -> TriState.DISABLED
        }
    }
fun Anime.chaptersFiltered(): Boolean {
    return unreadFilter != TriState.DISABLED ||
        downloadedFilter != TriState.DISABLED ||
        bookmarkedFilter != TriState.DISABLED
}
fun Anime.forceDownloaded(): Boolean {
    return favorite && Injekt.get<BasePreferences>().downloadedOnly().get()
}

fun Anime.toSManga(): SManga = SManga.create().also {
    it.url = url
    it.title = title
    it.artist = artist
    it.author = author
    it.description = description
    it.genre = genre.orEmpty().joinToString()
    it.status = status.toInt()
    it.thumbnail_url = thumbnailUrl
    it.initialized = initialized
}

fun Anime.copyFrom(other: SManga): Anime {
    // SY -->
    val author = other.author ?: ogAuthor
    val artist = other.artist ?: ogArtist
    val thumbnailUrl = other.thumbnail_url ?: ogThumbnailUrl
    val description = other.description ?: ogDescription
    val genres = if (other.genre != null) {
        other.getGenres()
    } else {
        ogGenre
    }
    // SY <--
    return this.copy(
        // SY -->
        ogAuthor = author,
        ogArtist = artist,
        ogThumbnailUrl = thumbnailUrl,
        ogDescription = description,
        ogGenre = genres,
        // SY <--
        // SY -->
        ogStatus = other.status.toLong(),
        // SY <--
        updateStrategy = other.update_strategy,
        initialized = other.initialized && initialized,
    )
}

fun SManga.toDomainManga(sourceId: Long): Anime {
    return Anime.create().copy(
        url = url,
        // SY -->
        ogTitle = title,
        ogArtist = artist,
        ogAuthor = author,
        ogThumbnailUrl = thumbnail_url,
        ogDescription = description,
        ogGenre = getGenres(),
        ogStatus = status.toLong(),
        // SY <--
        updateStrategy = update_strategy,
        initialized = initialized,
        source = sourceId,
    )
}

fun Anime.hasCustomCover(coverCache: CoverCache = Injekt.get()): Boolean {
    return coverCache.getCustomCoverFile(id).exists()
}

/**
 * Creates a ComicInfo instance based on the manga and episode metadata.
 */
fun getComicInfo(
    manga: Anime,
    episode: Episode,
    urls: List<String>,
    categories: List<String>?,
    sourceName: String,
) = ComicInfo(
    title = ComicInfo.Title(episode.name),
    series = ComicInfo.Series(manga.title),
    number = episode.chapterNumber.takeIf { it >= 0 }?.let {
        if ((it.rem(1) == 0.0)) {
            ComicInfo.Number(it.toInt().toString())
        } else {
            ComicInfo.Number(it.toString())
        }
    },
    web = ComicInfo.Web(urls.joinToString(" ")),
    summary = manga.description?.let { ComicInfo.Summary(it) },
    writer = manga.author?.let { ComicInfo.Writer(it) },
    penciller = manga.artist?.let { ComicInfo.Penciller(it) },
    translator = episode.scanlator?.let { ComicInfo.Translator(it) },
    genre = manga.genre?.let { ComicInfo.Genre(it.joinToString()) },
    publishingStatus = ComicInfo.PublishingStatusTachiyomi(
        ComicInfoPublishingStatus.toComicInfoValue(manga.status),
    ),
    categories = categories?.let { ComicInfo.CategoriesTachiyomi(it.joinToString()) },
    source = ComicInfo.SourceMihon(sourceName),
    // SY -->
    padding = CbzCrypto.createComicInfoPadding()?.let { ComicInfo.PaddingTachiyomiSY(it) },
    // SY <--
    inker = null,
    colorist = null,
    letterer = null,
    coverArtist = null,
    tags = null,
)
