package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.model.CustomMangaInfo
import tachiyomi.domain.anime.repository.CustomMangaRepository

class SetCustomMangaInfo(
    private val customMangaRepository: CustomMangaRepository,
) {

    fun set(mangaInfo: CustomMangaInfo) = customMangaRepository.set(mangaInfo)
}
