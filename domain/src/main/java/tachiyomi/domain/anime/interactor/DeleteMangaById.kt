package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.repository.MangaRepository

class DeleteMangaById(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(id: Long) {
        return mangaRepository.deleteManga(id)
    }
}
