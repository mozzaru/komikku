package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.anime.repository.AnimeRepository

class GetDuplicateLibraryAnime(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(manga: Manga): List<Manga> {
        return animeRepository.getDuplicateLibraryManga(manga.id, manga.title.lowercase())
    }
}
