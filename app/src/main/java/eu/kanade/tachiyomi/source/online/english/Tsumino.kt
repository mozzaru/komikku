package eu.kanade.tachiyomi.source.online.english

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.animesource.online.MetadataSource
import eu.kanade.tachiyomi.animesource.online.NamespaceSource
import eu.kanade.tachiyomi.animesource.online.UrlImportableSource
import eu.kanade.tachiyomi.util.asJsoup
import exh.metadata.metadata.RaisedSearchMetadata.Companion.TAG_TYPE_VIRTUAL
import exh.metadata.metadata.TsuminoSearchMetadata
import exh.metadata.metadata.TsuminoSearchMetadata.Companion.TAG_TYPE_DEFAULT
import exh.metadata.metadata.base.RaisedTag
import exh.source.DelegatedAnimeHttpSource
import exh.util.dropBlank
import exh.util.trimAll
import exh.util.urlImportFetchSearchManga
import exh.util.urlImportFetchSearchMangaSuspend
import org.jsoup.nodes.Document
import rx.Observable
import java.text.SimpleDateFormat
import java.util.Locale

class Tsumino(delegate: AnimeHttpSource, val context: Context) :
    DelegatedAnimeHttpSource(delegate),
    MetadataSource<TsuminoSearchMetadata, Document>,
    UrlImportableSource,
    NamespaceSource {
    override val metaClass = TsuminoSearchMetadata::class
    override fun newMetaInstance() = TsuminoSearchMetadata()
    override val lang = "en"

    // Support direct URL importing
    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getSearchAnime"))
    override fun fetchSearchAnime(page: Int, query: String, filters: AnimeFilterList): Observable<AnimesPage> =
        urlImportFetchSearchManga(context, query) {
            @Suppress("DEPRECATION")
            super<DelegatedAnimeHttpSource>.fetchSearchAnime(page, query, filters)
        }

    override suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList): AnimesPage {
        return urlImportFetchSearchMangaSuspend(context, query) {
            super<DelegatedAnimeHttpSource>.getSearchAnime(page, query, filters)
        }
    }

    override suspend fun mapUrlToMangaUrl(uri: Uri): String? {
        val lcFirstPathSegment = uri.pathSegments.firstOrNull()?.lowercase(Locale.ROOT) ?: return null
        if (lcFirstPathSegment != "read" && lcFirstPathSegment != "book" && lcFirstPathSegment != "entry") {
            return null
        }
        return "https://tsumino.com/Book/Info/${uri.lastPathSegment}"
    }

    override suspend fun getAnimeDetails(anime: SAnime): SAnime {
        val response = client.newCall(animeDetailsRequest(anime)).awaitSuccess()
        return parseToManga(anime, response.asJsoup())
    }

    override suspend fun parseIntoMetadata(metadata: TsuminoSearchMetadata, input: Document) {
        with(metadata) {
            tmId = TsuminoSearchMetadata.tmIdFromUrl(input.location())!!.toInt()
            tags.clear()

            input.select("meta[property=og:title]").firstOrNull()?.attr("content")?.let {
                title = it.trim()
            }

            input.getElementById("Artist")?.children()?.first()?.attr("data-define")?.trim()?.let { artistString ->
                artistString.split("|").trimAll().dropBlank().forEach {
                    tags.add(RaisedTag("artist", it, TAG_TYPE_DEFAULT))
                }
                tags.add(RaisedTag("artist", artistString, TAG_TYPE_VIRTUAL))
                artist = artistString
            }

            input.getElementById("Uploader")?.children()?.first()?.text()?.trim()?.let {
                uploader = it
            }

            input.getElementById("Uploaded")?.text()?.let {
                uploadDate = TM_DATE_FORMAT.parse(it.trim())!!.time
            }

            input.getElementById("Pages")?.text()?.let {
                length = it.trim().toIntOrNull()
            }

            input.getElementById("Rating")?.text()?.let {
                ratingString = it.trim()
                val ratingString = ratingString
                if (!ratingString.isNullOrBlank()) {
                    averageRating = RATING_FLOAT_REGEX.find(ratingString)?.groups?.get(1)?.value?.toFloatOrNull()
                    userRatings = RATING_USERS_REGEX.find(ratingString)?.groups?.get(1)?.value?.toLongOrNull()
                    favorites = RATING_FAVORITES_REGEX.find(ratingString)?.groups?.get(1)?.value?.toLongOrNull()
                }
            }

            input.getElementById("Category")?.children()?.first()?.attr("data-define")?.let {
                category = it.trim()
                tags.add(RaisedTag("genre", it, TAG_TYPE_VIRTUAL))
            }

            input.getElementById("Collection")?.children()?.first()?.attr("data-define")?.let {
                collection = it.trim()
                tags.add(RaisedTag("collection", it, TAG_TYPE_DEFAULT))
            }

            input.getElementById("Group")?.children()?.first()?.attr("data-define")?.let {
                group = it.trim()
                tags.add(RaisedTag("group", it, TAG_TYPE_DEFAULT))
            }

            parody = input.getElementById("Parody")?.children()?.map {
                val entry = it.attr("data-define").trim()
                tags.add(RaisedTag("parody", entry, TAG_TYPE_DEFAULT))
                entry
            }.orEmpty()

            character = input.getElementById("Character")?.children()?.map {
                val entry = it.attr("data-define").trim()
                tags.add(RaisedTag("character", entry, TAG_TYPE_DEFAULT))
                entry
            }.orEmpty()

            input.getElementById("Tag")?.children()?.let { tagElements ->
                tags.addAll(
                    tagElements.map {
                        RaisedTag("tags", it.attr("data-define").trim(), TAG_TYPE_DEFAULT)
                    },
                )
            }
        }
    }

    override val matchingHosts = listOf(
        "www.tsumino.com",
        "tsumino.com",
    )

    companion object {
        val TM_DATE_FORMAT = SimpleDateFormat("yyyy MMM dd", Locale.US)
        val RATING_FLOAT_REGEX = "([0-9].*) \\(".toRegex()
        val RATING_USERS_REGEX = "\\(([0-9].*) users".toRegex()
        val RATING_FAVORITES_REGEX = "/ ([0-9].*) favs".toRegex()
    }
}
