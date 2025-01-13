package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.repository.FavoritesEntryRepository

class DeleteFavoriteEntries(
    private val favoriteEntryRepository: FavoritesEntryRepository,
) {

    suspend fun await() {
        return favoriteEntryRepository.deleteAll()
    }
}
