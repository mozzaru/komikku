package tachiyomi.domain.anime.interactor

import eu.kanade.tachiyomi.source.online.MetadataSource
import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.anime.repository.AnimeRepository

class GetAnime(
    private val animeRepository: AnimeRepository,
) : MetadataSource.GetMangaId {

    suspend fun await(id: Long): Manga? {
        return try {
            animeRepository.getMangaById(id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }

    suspend fun subscribe(id: Long): Flow<Manga> {
        return animeRepository.getMangaByIdAsFlow(id)
    }

    fun subscribe(url: String, sourceId: Long): Flow<Manga?> {
        return animeRepository.getMangaByUrlAndSourceIdAsFlow(url, sourceId)
    }

    // SY -->
    suspend fun await(url: String, sourceId: Long): Manga? {
        return animeRepository.getMangaByUrlAndSourceId(url, sourceId)
    }

    override suspend fun awaitId(url: String, sourceId: Long): Long? {
        return await(url, sourceId)?.id
    }
    // SY <--
}
