package tachiyomi.domain.anime.interactor

import eu.kanade.tachiyomi.source.online.MetadataSource
import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.repository.AnimeRepository

class GetAnime(
    private val animeRepository: AnimeRepository,
) : MetadataSource.GetAnimeId {

    suspend fun await(id: Long): Anime? {
        return try {
            animeRepository.getAnimeById(id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }

    suspend fun subscribe(id: Long): Flow<Anime> {
        return animeRepository.getAnimeByIdAsFlow(id)
    }

    fun subscribe(url: String, sourceId: Long): Flow<Anime?> {
        return animeRepository.getAnimeByUrlAndSourceIdAsFlow(url, sourceId)
    }

    // SY -->
    suspend fun await(url: String, sourceId: Long): Anime? {
        return animeRepository.getAnimeByUrlAndSourceId(url, sourceId)
    }

    override suspend fun awaitId(url: String, sourceId: Long): Long? {
        return await(url, sourceId)?.id
    }
    // SY <--
}
