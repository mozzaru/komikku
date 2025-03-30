package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository

class GetDuplicateLibraryAnime(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(manga: Manga): List<Manga> {
        return mangaRepository.getDuplicateLibraryAnime(manga.id, manga.title.lowercase())
    }
}
