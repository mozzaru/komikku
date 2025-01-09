package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.anime.repository.MangaMetadataRepository

class GetExhFavoriteMangaWithMetadata(
    private val mangaMetadataRepository: MangaMetadataRepository,
) {

    suspend fun await(): List<Manga> {
        return mangaMetadataRepository.getExhFavoriteMangaWithMetadata()
    }
}
