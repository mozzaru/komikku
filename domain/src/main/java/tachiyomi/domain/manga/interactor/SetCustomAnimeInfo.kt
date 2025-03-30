package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.repository.CustomMangaRepository

class SetCustomAnimeInfo(
    private val customMangaRepository: CustomMangaRepository,
) {

    fun set(mangaInfo: CustomMangaInfo) = customMangaRepository.set(mangaInfo)
}
