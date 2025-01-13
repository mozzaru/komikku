package tachiyomi.domain.anime.interactor

class SetCustomAnimeInfo(
    private val customAnimeRepository: tachiyomi.domain.anime.repository.CustomAnimeRepository,
) {

    fun set(animeInfo: tachiyomi.domain.anime.model.CustomAnimeInfo) = customAnimeRepository.set(animeInfo)
}
