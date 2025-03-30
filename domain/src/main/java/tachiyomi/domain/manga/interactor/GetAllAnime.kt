package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.AnimeRepository

class GetAllAnime(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(): List<Manga> {
        return animeRepository.getAll()
    }
}
