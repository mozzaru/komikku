package tachiyomi.domain.anime.interactor

import exh.metadata.sql.models.SearchMetadata
import tachiyomi.domain.anime.repository.AnimeMetadataRepository

class GetSearchMetadata(
    private val animeMetadataRepository: AnimeMetadataRepository,
) {

    suspend fun await(mangaId: Long): SearchMetadata? {
        return animeMetadataRepository.getMetadataById(mangaId)
    }

    suspend fun await(): List<SearchMetadata> {
        return animeMetadataRepository.getSearchMetadata()
    }
}
