package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.AnimeRepository

class GetAnimeBySource(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(sourceId: Long): List<Manga> {
        return animeRepository.getAnimeBySourceId(sourceId)
    }
}
