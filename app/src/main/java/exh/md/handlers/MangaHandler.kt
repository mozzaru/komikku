package exh.md.handlers

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.source.model.SAnime
import eu.kanade.tachiyomi.source.model.SEpisode
import exh.md.dto.EpisodeDataDto
import exh.md.service.MangaDexService
import exh.md.utils.MdConstants
import exh.md.utils.MdUtil
import exh.md.utils.mdListCall
import exh.metadata.metadata.MangaDexSearchMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import rx.Observable
import tachiyomi.core.common.util.lang.runAsObservable
import tachiyomi.core.common.util.lang.withIOContext

class AnimeHandler(
    private val lang: String,
    private val service: MangaDexService,
    private val apiAnimeParser: ApiAnimeParser,
    private val followsHandler: FollowsHandler,
) {
    suspend fun getAnimeDetails(
        anime: SAnime,
        sourceId: Long,
        coverQuality: String,
        tryUsingFirstVolumeCover: Boolean,
        altTitlesInDesc: Boolean,
    ): SAnime {
        return coroutineScope {
            val animeId = MdUtil.getAnimeId(anime.url)
            val response = async(Dispatchers.IO) { service.viewAnime(animeId) }
            val simpleEpisodes = async(Dispatchers.IO) { getSimpleEpisodes(anime) }
            val statistics =
                async(Dispatchers.IO) {
                    kotlin.runCatching { service.animesRating(animeId) }.getOrNull()?.statistics?.get(animeId)
                }
            val responseData = response.await()
            val coverFileName = if (tryUsingFirstVolumeCover) {
                async(Dispatchers.IO) {
                    service.fetchFirstVolumeCover(responseData)
                }
            } else {
                null
            }
            apiAnimeParser.parseToAnime(
                anime,
                sourceId,
                responseData,
                simpleEpisodes.await(),
                statistics.await(),
                coverFileName?.await(),
                coverQuality,
                altTitlesInDesc,
            )
        }
    }

    fun fetchAnimeDetailsObservable(anime: SAnime, sourceId: Long, coverQuality: String, tryUsingFirstVolumeCover: Boolean, altTitlesInDesc: Boolean): Observable<SAnime> {
        return runAsObservable {
            getAnimeDetails(anime, sourceId, coverQuality, tryUsingFirstVolumeCover, altTitlesInDesc)
        }
    }

    fun fetchEpisodeListObservable(
        anime: SAnime,
        blockedGroups: String,
        blockedUploaders: String,
    ): Observable<List<SEpisode>> = runAsObservable {
        getEpisodeList(anime, blockedGroups, blockedUploaders)
    }

    suspend fun getEpisodeList(anime: SAnime, blockedGroups: String, blockedUploaders: String): List<SEpisode> {
        return withIOContext {
            val results = mdListCall {
                service.viewEpisodes(
                    MdUtil.getAnimeId(anime.url),
                    lang,
                    it,
                    blockedGroups,
                    blockedUploaders,
                )
            }

            val groupMap = getGroupMap(results)

            apiAnimeParser.episodeListParse(results, groupMap)
        }
    }

    private fun getGroupMap(results: List<EpisodeDataDto>): Map<String, String> {
        return results.map { episode -> episode.relationships }
            .flatten()
            .filter { it.type == MdConstants.Types.scanlator }
            .map { it.id to it.attributes!!.name!! }
            .toMap()
    }

    suspend fun fetchRandomAnimeId(): String {
        return withIOContext {
            service.randomAnime().data.id
        }
    }

    suspend fun getTrackingInfo(track: Track): Pair<Track, MangaDexSearchMetadata?> {
        return withIOContext {
            /*val metadata = async {
                val animeUrl = MdUtil.buildAnimeUrl(MdUtil.getAnimeId(track.tracking_url))
                val anime = AnimeInfo(animeUrl, track.title)
                val response = client.newCall(animeRequest(anime)).await()
                val metadata = MangaDexSearchMetadata()
                apiAnimeParser.parseIntoMetadata(metadata, response, emptyList())
                metadata
            }*/
            val remoteTrack = async {
                followsHandler.fetchTrackingInfo(track.tracking_url)
            }
            remoteTrack.await() to null
        }
    }

    suspend fun getAnimeFromEpisodeId(episodeId: String): String? {
        return withIOContext {
            apiAnimeParser.episodeParseForAnimeId(service.viewEpisode(episodeId))
        }
    }

    suspend fun getAnimeMetadata(
        track: Track,
        sourceId: Long,
        coverQuality: String,
        tryUsingFirstVolumeCover: Boolean,
        altTitlesInDesc: Boolean,
    ): SAnime? {
        return withIOContext {
            val animeId = MdUtil.getAnimeId(track.tracking_url)
            val response = service.viewAnime(animeId)
            val coverFileName = if (tryUsingFirstVolumeCover) {
                service.fetchFirstVolumeCover(response)
            } else {
                null
            }
            apiAnimeParser.parseToAnime(
                SAnime.create().apply {
                    url = track.tracking_url
                },
                sourceId,
                response,
                emptyList(),
                null,
                coverFileName,
                coverQuality,
                altTitlesInDesc,
            )
        }
    }

    private suspend fun getSimpleEpisodes(anime: SAnime): List<String> {
        return runCatching { service.aggregateEpisodes(MdUtil.getAnimeId(anime.url), lang) }
            .onFailure {
                if (it is CancellationException) throw it
            }
            .map { dto ->
                dto.volumes.values
                    .flatMap { it.episodes.values }
                    .map { it.episode }
            }
            .getOrElse { emptyList() }
    }
}
