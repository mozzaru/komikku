package exh.md.service

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import exh.md.dto.AnimeListDto
import exh.md.dto.RatingDto
import exh.md.dto.RatingResponseDto
import exh.md.dto.ReadEpisodeDto
import exh.md.dto.ReadingStatusDto
import exh.md.dto.ReadingStatusMapDto
import exh.md.dto.ResultDto
import exh.md.utils.MdApi
import exh.md.utils.MdConstants
import exh.md.utils.MdUtil
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class MangaDexAuthService(
    private val client: OkHttpClient,
    private val headers: Headers,
) {

    suspend fun userFollowList(offset: Int): AnimeListDto {
        return with(MdUtil.jsonParser) {
            client.newCall(
                GET(
                    "${MdApi.userFollows}?limit=100&offset=$offset&includes[]=${MdConstants.Types.coverArt}",
                    headers,
                    CacheControl.FORCE_NETWORK,
                ),
            ).awaitSuccess().parseAs()
        }
    }

    suspend fun readingStatusForAnime(animeId: String): ReadingStatusDto {
        return with(MdUtil.jsonParser) {
            client.newCall(
                GET(
                    "${MdApi.anime}/$animeId/status",
                    headers,
                    CacheControl.FORCE_NETWORK,
                ),
            ).awaitSuccess().parseAs()
        }
    }

    suspend fun readEpisodesForAnime(animeId: String): ReadEpisodeDto {
        return with(MdUtil.jsonParser) {
            client.newCall(
                GET(
                    "${MdApi.anime}/$animeId/read",
                    headers,
                    CacheControl.FORCE_NETWORK,
                ),
            ).awaitSuccess().parseAs()
        }
    }

    suspend fun updateReadingStatusForAnime(
        animeId: String,
        readingStatusDto: ReadingStatusDto,
    ): ResultDto {
        return with(MdUtil.jsonParser) {
            client.newCall(
                POST(
                    "${MdApi.anime}/$animeId/status",
                    headers,
                    body = MdUtil.encodeToBody(readingStatusDto),
                    cache = CacheControl.FORCE_NETWORK,
                ),
            ).awaitSuccess().parseAs()
        }
    }

    suspend fun readingStatusAllAnime(): ReadingStatusMapDto {
        return with(MdUtil.jsonParser) {
            client.newCall(
                GET(
                    MdApi.readingStatusForAllAnime,
                    headers,
                    cache = CacheControl.FORCE_NETWORK,
                ),
            ).awaitSuccess().parseAs()
        }
    }

    suspend fun readingStatusByType(status: String): ReadingStatusMapDto {
        return with(MdUtil.jsonParser) {
            client.newCall(
                GET(
                    "${MdApi.readingStatusForAllAnime}?status=$status",
                    headers,
                    cache = CacheControl.FORCE_NETWORK,
                ),
            ).awaitSuccess().parseAs()
        }
    }

    suspend fun markEpisodeRead(episodeId: String): ResultDto {
        return with(MdUtil.jsonParser) {
            client.newCall(
                POST(
                    "${MdApi.episode}/$episodeId/read",
                    headers,
                    cache = CacheControl.FORCE_NETWORK,
                ),
            ).awaitSuccess().parseAs()
        }
    }

    suspend fun markEpisodeUnRead(episodeId: String): ResultDto {
        return with(MdUtil.jsonParser) {
            client.newCall(
                Request.Builder()
                    .url("${MdApi.episode}/$episodeId/read")
                    .delete()
                    .headers(headers)
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .build(),
            ).awaitSuccess().parseAs()
        }
    }

    suspend fun followAnime(animeId: String): ResultDto {
        return with(MdUtil.jsonParser) {
            client.newCall(
                POST(
                    "${MdApi.anime}/$animeId/follow",
                    headers,
                    cache = CacheControl.FORCE_NETWORK,
                ),
            ).awaitSuccess().parseAs()
        }
    }

    suspend fun unfollowAnime(animeId: String): ResultDto {
        return with(MdUtil.jsonParser) {
            client.newCall(
                Request.Builder()
                    .url("${MdApi.anime}/$animeId/follow")
                    .delete()
                    .headers(headers)
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .build(),
            ).awaitSuccess().parseAs()
        }
    }

    suspend fun updateAnimeRating(animeId: String, rating: Int): ResultDto {
        return with(MdUtil.jsonParser) {
            client.newCall(
                POST(
                    "${MdApi.rating}/$animeId",
                    headers,
                    body = MdUtil.encodeToBody(RatingDto(rating)),
                    cache = CacheControl.FORCE_NETWORK,
                ),
            ).awaitSuccess().parseAs()
        }
    }

    suspend fun deleteAnimeRating(animeId: String): ResultDto {
        return with(MdUtil.jsonParser) {
            client.newCall(
                Request.Builder()
                    .delete()
                    .url("${MdApi.rating}/$animeId")
                    .headers(headers)
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .build(),
            ).awaitSuccess().parseAs()
        }
    }

    suspend fun animesRating(vararg animeIds: String): RatingResponseDto {
        return with(MdUtil.jsonParser) {
            client.newCall(
                GET(
                    MdApi.rating.toHttpUrl()
                        .newBuilder()
                        .apply {
                            animeIds.forEach {
                                addQueryParameter("anime[]", it)
                            }
                        }
                        .build(),
                    headers,
                    cache = CacheControl.FORCE_NETWORK,
                ),
            ).awaitSuccess().parseAs()
        }
    }
}
