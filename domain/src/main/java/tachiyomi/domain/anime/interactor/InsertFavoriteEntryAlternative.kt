package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.model.FavoriteEntryAlternative
import tachiyomi.domain.anime.repository.FavoritesEntryRepository

class InsertFavoriteEntryAlternative(
    private val favoriteEntryRepository: FavoritesEntryRepository,
) {

    suspend fun await(entry: FavoriteEntryAlternative) {
        return favoriteEntryRepository.addAlternative(entry)
    }
}
