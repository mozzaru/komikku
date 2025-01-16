package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.anime.repository.AnimeMetadataRepository

class GetExhFavoriteAnimeWithMetadata(
    private val animeMetadataRepository: AnimeMetadataRepository,
) {

    suspend fun await(): List<Manga> {
        return animeMetadataRepository.getExhFavoriteMangaWithMetadata()
    }
}
