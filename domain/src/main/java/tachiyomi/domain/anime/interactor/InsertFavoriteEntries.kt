package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.model.FavoriteEntry
import tachiyomi.domain.anime.repository.FavoritesEntryRepository

class InsertFavoriteEntries(
    private val favoriteEntryRepository: FavoritesEntryRepository,
) {

    suspend fun await(entries: List<FavoriteEntry>) {
        return favoriteEntryRepository.insertAll(entries)
    }
}
