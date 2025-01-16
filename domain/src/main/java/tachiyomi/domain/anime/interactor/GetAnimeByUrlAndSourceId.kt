package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.anime.repository.AnimeRepository

class GetAnimeByUrlAndSourceId(
    private val animeRepository: AnimeRepository,
) {
    suspend fun await(url: String, sourceId: Long): Manga? {
        return animeRepository.getMangaByUrlAndSourceId(url, sourceId)
    }
}
