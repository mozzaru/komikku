package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.repository.CustomAnimeRepository

class GetCustomMangaInfo(
    private val customAnimeRepository: CustomAnimeRepository,
) {

    fun get(mangaId: Long) = customAnimeRepository.get(mangaId)
}
