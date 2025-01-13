package exh.md.handlers

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.model.MetadataAnimesPage
import eu.kanade.tachiyomi.source.model.SAnime
import exh.md.dto.AnimeDataDto
import exh.md.dto.PersonalRatingDto
import exh.md.dto.ReadingStatusDto
import exh.md.service.MangaDexAuthService
import exh.md.utils.FollowStatus
import exh.md.utils.MdUtil
import exh.md.utils.asMdMap
import exh.md.utils.mdListCall
import exh.metadata.metadata.MangaDexSearchMetadata
import exh.util.under
import kotlinx.coroutines.async
import tachiyomi.core.common.util.lang.withIOContext

class FollowsHandler(
    private val lang: String,
    private val service: MangaDexAuthService,
) {

    /**
     * fetch follows page
     */
    suspend fun fetchFollows(page: Int): MetadataAnimesPage {
        return withIOContext {
            val follows = service.userFollowList(MdUtil.animeLimit * page)

            if (follows.data.isEmpty()) {
                return@withIOContext MetadataAnimesPage(emptyList(), false, emptyList())
            }

            val hasMoreResults = follows.limit + follows.offset under follows.total
            val statusListResponse = service.readingStatusAllAnime()
            val results = followsParseAnimePage(follows.data, statusListResponse.statuses)

            MetadataAnimesPage(results.map { it.first }, hasMoreResults, results.map { it.second })
        }
    }

    /**
     * Parse follows api to anime page
     * used when multiple follows
     */
    private fun followsParseAnimePage(
        response: List<AnimeDataDto>,
        statuses: Map<String, String?>,
    ): List<Pair<SAnime, MangaDexSearchMetadata>> {
        val comparator = compareBy<Pair<SAnime, MangaDexSearchMetadata>> { it.second.followStatus }
            .thenBy { it.first.title }

        return response.map {
            MdUtil.createAnimeEntry(
                it,
                lang,
            ) to MangaDexSearchMetadata().apply {
                followStatus = FollowStatus.fromDex(statuses[it.id]).long.toInt()
            }
        }.sortedWith(comparator)
    }

    /**
     * Change the status of a anime
     */
    suspend fun updateFollowStatus(animeId: String, followStatus: FollowStatus): Boolean {
        return withIOContext {
            val status = when (followStatus == FollowStatus.UNFOLLOWED) {
                true -> null
                false -> followStatus.toDex()
            }
            val readingStatusDto = ReadingStatusDto(status)

            if (followStatus == FollowStatus.UNFOLLOWED) {
                service.unfollowAnime(animeId)
            } else {
                service.followAnime(animeId)
            }

            service.updateReadingStatusForAnime(animeId, readingStatusDto).result == "ok"
        }
    }

    /*suspend fun updateReadingProgress(track: Track): Boolean {
        return true
        return withIOContext {
            val animeID = getAnimeId(track.tracking_url)
            val formBody = FormBody.Builder()
                .add("volume", "0")
                .add("episode", track.last_episode_read.toString())
            XLog.d("episode to update %s", track.last_episode_read.toString())
            val result = runCatching {
                client.newCall(
                    POST(
                        "$baseUrl/ajax/actions.ajax.php?function=edit_progress&id=$animeID",
                        headers,
                        formBody.build()
                    )
                ).execute()
            }
            result.exceptionOrNull()?.let {
                if (it is EOFException) {
                    return@withIOContext true
                } else {
                    XLog.e("error updating reading progress", it)
                    return@withIOContext false
                }
            }
            result.isSuccess
        }
    }*/

    suspend fun updateRating(track: Track): Boolean {
        return withIOContext {
            val animeId = MdUtil.getAnimeId(track.tracking_url)
            val result = runCatching {
                if (track.score == 0.0) {
                    service.deleteAnimeRating(animeId)
                } else {
                    service.updateAnimeRating(animeId, track.score.toInt())
                }.result == "ok"
            }
            result.getOrDefault(false)
        }
    }

    /**
     * fetch all anime from all possible pages
     */
    suspend fun fetchAllFollows(): List<Pair<SAnime, MangaDexSearchMetadata>> {
        return withIOContext {
            val results = async {
                mdListCall {
                    service.userFollowList(it)
                }
            }

            val readingStatusResponse = async { service.readingStatusAllAnime().statuses }

            followsParseAnimePage(results.await(), readingStatusResponse.await())
        }
    }

    suspend fun fetchTrackingInfo(url: String): Track {
        return withIOContext {
            val animeId = MdUtil.getAnimeId(url)
            val followStatusDef = async {
                FollowStatus.fromDex(service.readingStatusForAnime(animeId).status)
            }
            val ratingDef = async {
                service.animesRating(animeId).ratings.asMdMap<PersonalRatingDto>()[animeId]
            }
            val (followStatus, rating) = followStatusDef.await() to ratingDef.await()
            Track.create(TrackerManager.MDLIST).apply {
                title = ""
                status = followStatus.long
                tracking_url = url
                score = rating?.rating?.toDouble() ?: 0.0
            }
        }
    }
}
