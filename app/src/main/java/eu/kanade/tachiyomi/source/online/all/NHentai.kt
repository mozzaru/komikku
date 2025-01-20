package eu.kanade.tachiyomi.source.online.all

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.newCachelessCallWithProgress
import eu.kanade.tachiyomi.animesource.ThumbnailPreviewInfo
import eu.kanade.tachiyomi.animesource.ThumbnailPreviewImage
import eu.kanade.tachiyomi.animesource.ThumbnailPreviewSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.animesource.online.MetadataSource
import eu.kanade.tachiyomi.animesource.online.NamespaceSource
import eu.kanade.tachiyomi.animesource.online.UrlImportableSource
import exh.metadata.metadata.NHentaiSearchMetadata
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.base.RaisedTag
import exh.source.DelegatedAnimeHttpSource
import exh.source.NHENTAI_SOURCE_ID
import exh.util.trimOrNull
import exh.util.urlImportFetchSearchManga
import exh.util.urlImportFetchSearchMangaSuspend
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.Response

class NHentai(delegate: AnimeHttpSource, val context: Context) :
    DelegatedAnimeHttpSource(delegate),
    MetadataSource<NHentaiSearchMetadata, Response>,
    UrlImportableSource,
    NamespaceSource,
    ThumbnailPreviewSource {
    override val metaClass = NHentaiSearchMetadata::class
    override fun newMetaInstance() = NHentaiSearchMetadata()
    override val lang = delegate.lang

    private val sourcePreferences: SharedPreferences by lazy {
        context.getSharedPreferences("source_$id", 0x0000)
    }

    private val preferredTitle: Int
        get() = when (sourcePreferences.getString(TITLE_PREF, "full")) {
            "full" -> NHentaiSearchMetadata.TITLE_TYPE_ENGLISH
            else -> NHentaiSearchMetadata.TITLE_TYPE_SHORT
        }

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
        return parseToManga(anime, response)
    }

    override suspend fun parseIntoMetadata(metadata: NHentaiSearchMetadata, input: Response) {
        // AZ -->
        val strdata = input.body.string()
        val server = MEDIA_SERVER_REGEX.find(strdata)?.groupValues?.get(1)?.toInt() ?: 1
        // AZ <--
        val json = GALLERY_JSON_REGEX.find(strdata)!!.groupValues[1].replace(
            UNICODE_ESCAPE_REGEX,
        ) { it.groupValues[1].toInt(radix = 16).toChar().toString() }
        val jsonResponse = jsonParser.decodeFromString<JsonResponse>(json)

        with(metadata) {
            nhId = jsonResponse.id

            uploadDate = jsonResponse.uploadDate

            favoritesCount = jsonResponse.numFavorites

            mediaId = jsonResponse.mediaId

            // AZ -->
            mediaServer = server
            // AZ <--

            jsonResponse.title?.let { title ->
                japaneseTitle = title.japanese
                shortTitle = title.pretty
                englishTitle = title.english
            }

            preferredTitle = this@NHentai.preferredTitle

            jsonResponse.images?.let { images ->
                coverImageType = images.cover?.type
                images.pages.mapNotNull {
                    it.type
                }.let {
                    pageImageTypes = it
                }
                thumbnailImageType = images.thumbnail?.type
            }

            scanlator = jsonResponse.scanlator?.trimOrNull()

            tags.clear()
            jsonResponse.tags.filter {
                it.type != null && it.name != null
            }.mapTo(tags) {
                RaisedTag(
                    it.type!!,
                    it.name!!,
                    if (it.type == NHentaiSearchMetadata.NHENTAI_CATEGORIES_NAMESPACE) {
                        RaisedSearchMetadata.TAG_TYPE_VIRTUAL
                    } else {
                        NHentaiSearchMetadata.TAG_TYPE_DEFAULT
                    },
                )
            }
        }
    }

    @Serializable
    data class JsonResponse(
        val id: Long,
        @SerialName("media_id")
        val mediaId: String? = null,
        val title: JsonTitle? = null,
        val images: JsonImages? = null,
        val scanlator: String? = null,
        @SerialName("upload_date")
        val uploadDate: Long? = null,
        val tags: List<JsonTag> = emptyList(),
        @SerialName("num_pages")
        val numPages: Int? = null,
        @SerialName("num_favorites")
        val numFavorites: Long? = null,
    )

    @Serializable
    data class JsonTitle(
        val english: String? = null,
        val japanese: String? = null,
        val pretty: String? = null,
    )

    @Serializable
    data class JsonImages(
        val pages: List<JsonPage> = emptyList(),
        val cover: JsonPage? = null,
        val thumbnail: JsonPage? = null,
    )

    @Serializable
    data class JsonPage(
        @SerialName("t")
        val type: String? = null,
        @SerialName("w")
        val width: Long? = null,
        @SerialName("h")
        val height: Long? = null,
    )

    @Serializable
    data class JsonTag(
        val id: Long? = null,
        val type: String? = null,
        val name: String? = null,
        val url: String? = null,
        val count: Long? = null,
    )

    override val matchingHosts = listOf(
        "nhentai.net",
    )

    override suspend fun mapUrlToMangaUrl(uri: Uri): String? {
        if (uri.pathSegments.firstOrNull()?.lowercase() != "g") {
            return null
        }

        return "$baseUrl/g/${uri.pathSegments[1]}/"
    }

    override suspend fun getThumbnailPreviewList(anime: SAnime, episodes: List<SEpisode>, page: Int): ThumbnailPreviewImage {
        val metadata = fetchOrLoadMetadata(anime.id()) {
            client.newCall(animeDetailsRequest(anime)).awaitSuccess()
        }
        return ThumbnailPreviewImage(
            page,
            metadata.pageImageTypes.mapIndexed { index, s ->
                ThumbnailPreviewInfo(
                    index + 1,
                    imageUrl = thumbnailUrlFromType(
                        metadata.mediaId!!,
                        // AZ -->
                        metadata.mediaServer ?: 1,
                        // AZ <--
                        index + 1,
                        s,
                    )!!,
                )
            },
            false,
            1,
        )
    }

    private fun thumbnailUrlFromType(
        mediaId: String,
        // AZ -->
        mediaServer: Int,
        // AZ <--
        page: Int,
        t: String,
    ) = NHentaiSearchMetadata.typeToExtension(t)?.let {
        "https://t$mediaServer.nhentai.net/galleries/$mediaId/${page}t.$it"
    }

    override suspend fun fetchPreviewImage(page: ThumbnailPreviewInfo, cacheControl: CacheControl?): Response {
        return client.newCachelessCallWithProgress(
            if (cacheControl != null) {
                GET(page.imageUrl, cache = cacheControl)
            } else {
                GET(page.imageUrl)
            },
            page,
        ).awaitSuccess()
    }

    companion object {
        const val otherId = NHENTAI_SOURCE_ID

        private val jsonParser = Json {
            ignoreUnknownKeys = true
        }

        private val GALLERY_JSON_REGEX = Regex(".parse\\(\"(.*)\"\\);")

        // AZ -->
        private val MEDIA_SERVER_REGEX = Regex("media_server\\s*:\\s*(\\d+)")

        // AZ <--
        private val UNICODE_ESCAPE_REGEX = Regex("\\\\u([0-9a-fA-F]{4})")
        private const val TITLE_PREF = "Display anime title as:"
    }
}
