package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.repository.AnimeRepository

class ResetViewerFlags(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(): Boolean {
        return animeRepository.resetViewerFlags()
    }
}
