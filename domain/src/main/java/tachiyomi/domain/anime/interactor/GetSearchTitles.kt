package tachiyomi.domain.anime.interactor

import exh.metadata.sql.models.SearchTitle

class GetSearchTitles(
    private val animeMetadataRepository: tachiyomi.domain.anime.repository.AnimeMetadataRepository,
) {

    suspend fun await(animeId: Long): List<SearchTitle> {
        return animeMetadataRepository.getTitlesById(animeId)
    }
}
