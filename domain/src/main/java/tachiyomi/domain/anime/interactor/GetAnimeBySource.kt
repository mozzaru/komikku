package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.anime.repository.AnimeRepository

class GetAnimeBySource(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(sourceId: Long): List<Manga> {
        return animeRepository.getMangaBySourceId(sourceId)
    }
}
