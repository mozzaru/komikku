package tachiyomi.domain.manga.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.AnimeRepository

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
