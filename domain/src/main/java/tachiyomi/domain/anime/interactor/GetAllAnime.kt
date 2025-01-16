package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.anime.repository.AnimeRepository

class GetAllAnime(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(): List<Manga> {
        return animeRepository.getAll()
    }
}
