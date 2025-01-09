package tachiyomi.domain.anime.interactor

import exh.metadata.sql.models.SearchMetadata
import tachiyomi.domain.anime.repository.MangaMetadataRepository

class GetSearchMetadata(
    private val mangaMetadataRepository: MangaMetadataRepository,
) {

    suspend fun await(mangaId: Long): SearchMetadata? {
        return mangaMetadataRepository.getMetadataById(mangaId)
    }

    suspend fun await(): List<SearchMetadata> {
        return mangaMetadataRepository.getSearchMetadata()
    }
}
