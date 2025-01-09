package tachiyomi.domain.anime.interactor

import exh.metadata.sql.models.SearchTag
import tachiyomi.domain.anime.repository.MangaMetadataRepository

class GetSearchTags(
    private val mangaMetadataRepository: MangaMetadataRepository,
) {

    suspend fun await(mangaId: Long): List<SearchTag> {
        return mangaMetadataRepository.getTagsById(mangaId)
    }
}
