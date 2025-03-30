package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.library.model.LibraryManga

class GetSeenAnimeNotInLibraryView(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(): List<LibraryManga> {
        return mangaRepository.getReadMangaNotInLibraryView()
    }
}
