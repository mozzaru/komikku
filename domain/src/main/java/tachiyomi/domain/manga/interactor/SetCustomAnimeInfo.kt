package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.CustomAnimeInfo
import tachiyomi.domain.manga.repository.CustomAnimeRepository

class SetCustomAnimeInfo(
    private val customAnimeRepository: CustomAnimeRepository,
) {

    fun set(mangaInfo: CustomAnimeInfo) = customAnimeRepository.set(mangaInfo)
}
