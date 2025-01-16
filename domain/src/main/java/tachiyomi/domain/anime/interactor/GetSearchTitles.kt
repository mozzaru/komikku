package tachiyomi.domain.anime.interactor

import exh.metadata.sql.models.SearchTitle
import tachiyomi.domain.anime.repository.AnimeMetadataRepository

class GetSearchTitles(
    private val animeMetadataRepository: AnimeMetadataRepository,
) {

    suspend fun await(mangaId: Long): List<SearchTitle> {
        return animeMetadataRepository.getTitlesById(mangaId)
    }
}
