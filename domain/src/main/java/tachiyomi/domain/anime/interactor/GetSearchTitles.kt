package tachiyomi.domain.anime.interactor

import exh.metadata.sql.models.SearchTitle
import tachiyomi.domain.anime.repository.MangaMetadataRepository

class GetSearchTitles(
    private val mangaMetadataRepository: MangaMetadataRepository,
) {

    suspend fun await(mangaId: Long): List<SearchTitle> {
        return mangaMetadataRepository.getTitlesById(mangaId)
    }
}
