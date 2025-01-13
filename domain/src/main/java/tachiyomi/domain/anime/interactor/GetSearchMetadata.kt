package tachiyomi.domain.anime.interactor

import exh.metadata.sql.models.SearchMetadata

class GetSearchMetadata(
    private val animeMetadataRepository: tachiyomi.domain.anime.repository.AnimeMetadataRepository,
) {

    suspend fun await(animeId: Long): SearchMetadata? {
        return animeMetadataRepository.getMetadataById(animeId)
    }

    suspend fun await(): List<SearchMetadata> {
        return animeMetadataRepository.getSearchMetadata()
    }
}
