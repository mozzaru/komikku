package tachiyomi.domain.anime.interactor

import exh.metadata.sql.models.SearchTag

class GetSearchTags(
    private val animeMetadataRepository: tachiyomi.domain.anime.repository.AnimeMetadataRepository,
) {

    suspend fun await(animeId: Long): List<SearchTag> {
        return animeMetadataRepository.getTagsById(animeId)
    }
}
