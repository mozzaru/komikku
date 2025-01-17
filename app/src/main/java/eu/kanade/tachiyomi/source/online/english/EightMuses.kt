package eu.kanade.tachiyomi.source.online.english

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.animesource.online.MetadataSource
import eu.kanade.tachiyomi.animesource.online.NamespaceSource
import eu.kanade.tachiyomi.animesource.online.UrlImportableSource
import eu.kanade.tachiyomi.util.asJsoup
import exh.metadata.metadata.EightMusesSearchMetadata
import exh.metadata.metadata.base.RaisedTag
import exh.source.DelegatedAnimeHttpSource
import exh.util.urlImportFetchSearchManga
import exh.util.urlImportFetchSearchMangaSuspend
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class EightMuses(delegate: AnimeHttpSource, val context: Context) :
    DelegatedAnimeHttpSource(delegate),
    MetadataSource<EightMusesSearchMetadata, Document>,
    UrlImportableSource,
    NamespaceSource {
    override val metaClass = EightMusesSearchMetadata::class
    override fun newMetaInstance() = EightMusesSearchMetadata()
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

    data class SelfContents(val albums: List<Element>, val images: List<Element>)

    private fun parseSelf(doc: Document): SelfContents {
        // Parse self
        val gc = doc.select(".gallery .c-tile")

        // Check if any in self
        val selfAlbums = gc.filter { element -> element.attr("href").startsWith("/comics/album") }
        val selfImages = gc.filter { element -> element.attr("href").startsWith("/comics/picture") }

        return SelfContents(selfAlbums, selfImages)
    }

    override suspend fun parseIntoMetadata(metadata: EightMusesSearchMetadata, input: Document) {
        with(metadata) {
            path = input.location().toUri().pathSegments

            val breadcrumbs = input.selectFirst(".top-menu-breadcrumb > ol")

            title = breadcrumbs!!.selectFirst("li:nth-last-child(1) > a")!!.text()

            thumbnailUrl = parseSelf(input).let { it.albums + it.images }.firstOrNull()
                ?.selectFirst(".lazyload")
                ?.attr("data-src")?.let {
                    baseUrl + it
                }

            tags.clear()
            tags += RaisedTag(
                EightMusesSearchMetadata.ARTIST_NAMESPACE,
                breadcrumbs.selectFirst("li:nth-child(2) > a")!!.text(),
                EightMusesSearchMetadata.TAG_TYPE_DEFAULT,
            )
            tags += input.select(".album-tags a").map {
                RaisedTag(
                    EightMusesSearchMetadata.TAGS_NAMESPACE,
                    it.text(),
                    EightMusesSearchMetadata.TAG_TYPE_DEFAULT,
                )
            }
        }
    }

    override val matchingHosts = listOf(
        "www.8muses.com",
        "comics.8muses.com",
        "8muses.com",
    )

    override suspend fun mapUrlToMangaUrl(uri: Uri): String {
        var path = uri.pathSegments.drop(2)
        if (uri.pathSegments[1].lowercase() == "picture") {
            path = path.dropLast(1)
        }
        return "/comics/album/${path.joinToString("/")}"
    }
}
