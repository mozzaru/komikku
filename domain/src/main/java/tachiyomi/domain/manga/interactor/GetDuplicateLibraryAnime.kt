package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.AnimeRepository

class GetDuplicateLibraryAnime(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(manga: Manga): List<Manga> {
        return animeRepository.getDuplicateLibraryAnime(manga.id, manga.title.lowercase())
    }
}
