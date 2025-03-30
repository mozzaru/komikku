package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.repository.AnimeRepository
import tachiyomi.domain.library.model.LibraryAnime

class GetSeenAnimeNotInLibraryView(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(): List<LibraryAnime> {
        return animeRepository.getSeenAnimeNotInLibraryView()
    }
}
