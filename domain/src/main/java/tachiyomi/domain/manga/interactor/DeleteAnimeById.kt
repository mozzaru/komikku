package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.repository.AnimeRepository

class DeleteAnimeById(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(id: Long) {
        return animeRepository.deleteAnime(id)
    }
}
