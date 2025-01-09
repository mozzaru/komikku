package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.model.FavoriteEntry
import tachiyomi.domain.anime.repository.FavoritesEntryRepository

class GetFavoriteEntries(
    private val favoriteEntryRepository: FavoritesEntryRepository,
) {

    suspend fun await(): List<FavoriteEntry> {
        return favoriteEntryRepository.selectAll()
    }
}
