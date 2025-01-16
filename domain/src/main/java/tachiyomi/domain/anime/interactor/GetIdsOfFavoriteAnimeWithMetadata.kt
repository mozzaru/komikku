package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.repository.AnimeMetadataRepository

class GetIdsOfFavoriteAnimeWithMetadata(
    private val animeMetadataRepository: AnimeMetadataRepository,
) {

    suspend fun await(): List<Long> {
        return animeMetadataRepository.getIdsOfFavoriteMangaWithMetadata()
    }
}
