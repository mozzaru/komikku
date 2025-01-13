package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.model.Anime

class GetExhFavoriteAnimeWithMetadata(
    private val animeMetadataRepository: tachiyomi.domain.anime.repository.AnimeMetadataRepository,
) {

    suspend fun await(): List<Anime> {
        return animeMetadataRepository.getExhFavoriteAnimeWithMetadata()
    }
}
