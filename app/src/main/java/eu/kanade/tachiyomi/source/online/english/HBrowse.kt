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
import exh.metadata.metadata.HBrowseSearchMetadata
import exh.metadata.metadata.base.RaisedTag
import exh.source.DelegatedAnimeHttpSource
import exh.util.urlImportFetchSearchManga
import exh.util.urlImportFetchSearchMangaSuspend
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class HBrowse(delegate: AnimeHttpSource, val context: Context) :
    DelegatedAnimeHttpSource(delegate),
    MetadataSource<HBrowseSearchMetadata, Document>,
    UrlImportableSource,
    NamespaceSource {
    override val metaClass = HBrowseSearchMetadata::class
    override fun newMetaInstance() = HBrowseSearchMetadata()
    override val lang = "en"

    // Support direct URL importing
    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getSearchAnime"))
    override fun fetchSearchAnime(page: Int, query: String, filters: AnimeFilterList) =
        urlImportFetchSearchManga(context, query) {
            @Suppress("DEPRECATION")
            super<DelegatedAnimeHttpSource>.fetchSearchAnime(page, query, filters)
        }

    override suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList): AnimesPage {
        return urlImportFetchSearchMangaSuspend(context, query) {
            super<DelegatedAnimeHttpSource>.getSearchAnime(page, query, filters)
        }
    }

    override suspend fun getAnimeDetails(anime: SAnime): SAnime {
        val response = client.newCall(animeDetailsRequest(anime)).awaitSuccess()
        return parseToManga(anime, response.asJsoup())
    }

    override suspend fun parseIntoMetadata(metadata: HBrowseSearchMetadata, input: Document) {
        val tables = parseIntoTables(input)
        with(metadata) {
            hbUrl = input.location().removePrefix("$baseUrl/thumbnails")

            hbId = hbUrl!!.removePrefix("/").substringBefore("/").toLong()

            tags.clear()
            ((tables[""] ?: error("")) + (tables["categories"] ?: error(""))).forEach { (k, v) ->
                when (val lowercaseNs = k.lowercase()) {
                    "title" -> title = v.text()
                    "length" -> length = v.text().substringBefore(" ").toInt()
                    else -> {
                        v.getElementsByTag("a").forEach {
                            tags += RaisedTag(
                                lowercaseNs,
                                it.text(),
                                HBrowseSearchMetadata.TAG_TYPE_DEFAULT,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun parseIntoTables(doc: Document): Map<String, Map<String, Element>> {
        return doc.select("#main > .listTable").associate { ele ->
            val tableName = ele.previousElementSibling()?.text()?.lowercase().orEmpty()
            tableName to ele.select("tr")
                .filter { element -> element.childrenSize() > 1 }
                .associate {
                    it.child(0).text() to it.child(1)
                }
        }
    }

    override val matchingHosts = listOf(
        "www.hbrowse.com",
        "hbrowse.com",
    )

    override suspend fun mapUrlToMangaUrl(uri: Uri): String? {
        return uri.pathSegments.firstOrNull()?.let { "/$it/c00001/" }
    }
}
