package exh.md.service

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import exh.md.dto.AggregateDto
import exh.md.dto.AnimeDto
import exh.md.dto.AnimeListDto
import exh.md.dto.AtHomeDto
import exh.md.dto.AtHomeImageReportDto
import exh.md.dto.CoverListDto
import exh.md.dto.EpisodeDto
import exh.md.dto.EpisodeListDto
import exh.md.dto.RelationListDto
import exh.md.dto.ResultDto
import exh.md.dto.StatisticsDto
import exh.md.utils.MdApi
import exh.md.utils.MdConstants
import exh.md.utils.MdUtil
import exh.util.dropEmpty
import exh.util.trimAll
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

class MangaDexService(
    private val client: OkHttpClient,
) {

    suspend fun viewAnimes(
        ids: List<String>,
    ): AnimeListDto {
        return with(MdUtil.jsonParser) {
            client.newCall(
                GET(
                    MdApi.anime.toHttpUrl()
                        .newBuilder()
                        .apply {
                            addQueryParameter("includes[]", MdConstants.Types.coverArt)
                            addQueryParameter("limit", ids.size.toString())
                            ids.forEach {
                                addQueryParameter("ids[]", it)
                            }
                        }
                        .build(),
                    cache = CacheControl.FORCE_NETWORK,
                ),
            ).awaitSuccess().parseAs()
        }
    }

    suspend fun viewAnime(
        id: String,
    ): AnimeDto {
        return with(MdUtil.jsonParser) {
            client.newCall(
                GET(
                    MdApi.anime.toHttpUrl()
                        .newBuilder()
                        .apply {
                            addPathSegment(id)
                            addQueryParameter("includes[]", MdConstants.Types.coverArt)
                            addQueryParameter("includes[]", MdConstants.Types.author)
                            addQueryParameter("includes[]", MdConstants.Types.artist)
                        }
                        .build(),
                    cache = CacheControl.FORCE_NETWORK,
                ),
            ).awaitSuccess().parseAs()
        }
    }

    suspend fun animesRating(
        vararg ids: String,
    ): StatisticsDto {
        return with(MdUtil.jsonParser) {
            client.newCall(
                GET(
                    MdApi.statistics.toHttpUrl()
                        .newBuilder()
                        .apply {
                            ids.forEach { id ->
                                addQueryParameter("anime[]", id)
                            }
                        }
                        .build(),
                    cache = CacheControl.FORCE_NETWORK,
                ),
            ).awaitSuccess().parseAs()
        }
    }

    suspend fun aggregateEpisodes(
        id: String,
        translatedLanguage: String,
    ): AggregateDto {
        return with(MdUtil.jsonParser) {
            client.newCall(
                GET(
                    MdApi.anime.toHttpUrl()
                        .newBuilder()
                        .apply {
                            addPathSegment(id)
                            addPathSegment("aggregate")
                            addQueryParameter("translatedLanguage[]", translatedLanguage)
                        }
                        .build(),
                    cache = CacheControl.FORCE_NETWORK,
                ),
            ).awaitSuccess().parseAs()
        }
    }

    private fun String.splitString() = replace("\n", "").split(',').trimAll().dropEmpty()

    suspend fun viewEpisodes(
        id: String,
        translatedLanguage: String,
        offset: Int,
        blockedGroups: String,
        blockedUploaders: String,
    ): EpisodeListDto {
        val url = MdApi.anime.toHttpUrl()
            .newBuilder()
            .apply {
                addPathSegment(id)
                addPathSegment("feed")
                addQueryParameter("limit", "500")
                addQueryParameter("includes[]", MdConstants.Types.scanlator)
                addQueryParameter("order[volume]", "desc")
                addQueryParameter("order[episode]", "desc")
                addQueryParameter("contentRating[]", "safe")
                addQueryParameter("contentRating[]", "suggestive")
                addQueryParameter("contentRating[]", "erotica")
                addQueryParameter("contentRating[]", "pornographic")
                addQueryParameter("translatedLanguage[]", translatedLanguage)
                addQueryParameter("offset", offset.toString())
                blockedGroups.splitString().forEach {
                    addQueryParameter("excludedGroups[]", it)
                }
                blockedUploaders.splitString().forEach {
                    addQueryParameter("excludedUploaders[]", it)
                }
            }
            .build()

        return with(MdUtil.jsonParser) {
            client.newCall(
                GET(
                    url,
                    cache = CacheControl.FORCE_NETWORK,
                ),
            ).awaitSuccess().parseAs()
        }
    }

    suspend fun viewEpisode(id: String): EpisodeDto {
        return with(MdUtil.jsonParser) {
            client.newCall(GET("${MdApi.episode}/$id", cache = CacheControl.FORCE_NETWORK))
                .awaitSuccess()
                .parseAs()
        }
    }

    suspend fun randomAnime(): AnimeDto {
        return with(MdUtil.jsonParser) {
            client.newCall(GET("${MdApi.anime}/random", cache = CacheControl.FORCE_NETWORK))
                .awaitSuccess()
                .parseAs()
        }
    }

    suspend fun atHomeImageReport(atHomeImageReportDto: AtHomeImageReportDto): ResultDto {
        return with(MdUtil.jsonParser) {
            client.newCall(
                POST(
                    MdConstants.atHomeReportUrl,
                    body = MdUtil.encodeToBody(atHomeImageReportDto),
                    cache = CacheControl.FORCE_NETWORK,
                ),
            ).awaitSuccess().parseAs()
        }
    }

    suspend fun getAtHomeServer(
        atHomeRequestUrl: String,
        headers: Headers,
    ): AtHomeDto {
        return with(MdUtil.jsonParser) {
            client.newCall(GET(atHomeRequestUrl, headers, CacheControl.FORCE_NETWORK))
                .awaitSuccess()
                .parseAs()
        }
    }

    suspend fun relatedAnime(id: String): RelationListDto {
        return with(MdUtil.jsonParser) {
            client.newCall(
                GET(
                    MdApi.anime.toHttpUrl().newBuilder()
                        .apply {
                            addPathSegment(id)
                            addPathSegment("relation")
                        }
                        .build(),
                    cache = CacheControl.FORCE_NETWORK,
                ),
            ).awaitSuccess().parseAs()
        }
    }

    suspend fun fetchFirstVolumeCover(animeDto: AnimeDto): String? {
        val animeData = animeDto.data
        val result: CoverListDto = with(MdUtil.jsonParser) {
            client.newCall(
                GET(
                    MdApi.cover.toHttpUrl().newBuilder()
                        .apply {
                            addQueryParameter("order[volume]", "asc")
                            addQueryParameter("anime[]", animeData.id)
                            addQueryParameter("locales[]", animeData.attributes.originalLanguage)
                            addQueryParameter("limit", "1")
                        }
                        .build(),
                ),
            ).awaitSuccess().parseAs()
        }
        return result.data.firstOrNull()?.attributes?.fileName
    }
}
