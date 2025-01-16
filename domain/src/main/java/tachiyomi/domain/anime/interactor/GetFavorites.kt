package tachiyomi.domain.anime.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.anime.repository.AnimeRepository

class GetFavorites(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(): List<Manga> {
        return animeRepository.getFavorites()
    }

    fun subscribe(sourceId: Long): Flow<List<Manga>> {
        return animeRepository.getFavoritesBySourceId(sourceId)
    }
}
