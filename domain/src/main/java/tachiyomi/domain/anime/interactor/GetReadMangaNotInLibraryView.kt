package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.repository.MangaRepository
import tachiyomi.domain.library.model.LibraryManga

class GetReadMangaNotInLibraryView(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(): List<LibraryManga> {
        return mangaRepository.getReadMangaNotInLibraryView()
    }
}
