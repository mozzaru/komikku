package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.repository.AnimeMetadataRepository

class GetExhFavoriteAnimeWithMetadata(
    private val animeMetadataRepository: AnimeMetadataRepository,
) {

    suspend fun await(): List<Anime> {
        return animeMetadataRepository.getExhFavoriteAnimeWithMetadata()
    }
}
