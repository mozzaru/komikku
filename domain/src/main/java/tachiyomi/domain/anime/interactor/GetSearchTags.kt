package tachiyomi.domain.anime.interactor

import exh.metadata.sql.models.SearchTag
import tachiyomi.domain.anime.repository.AnimeMetadataRepository

class GetSearchTags(
    private val animeMetadataRepository: AnimeMetadataRepository,
) {

    suspend fun await(mangaId: Long): List<SearchTag> {
        return animeMetadataRepository.getTagsById(mangaId)
    }
}
