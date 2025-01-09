package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.repository.MangaMetadataRepository

class GetIdsOfFavoriteMangaWithMetadata(
    private val mangaMetadataRepository: MangaMetadataRepository,
) {

    suspend fun await(): List<Long> {
        return mangaMetadataRepository.getIdsOfFavoriteMangaWithMetadata()
    }
}
