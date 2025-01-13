package tachiyomi.domain.anime.interactor

class GetCustomAnimeInfo(
    private val customAnimeRepository: tachiyomi.domain.anime.repository.CustomAnimeRepository,
) {

    fun get(animeId: Long) = customAnimeRepository.get(animeId)
}
