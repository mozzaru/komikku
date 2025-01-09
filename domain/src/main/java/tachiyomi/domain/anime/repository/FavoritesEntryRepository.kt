package tachiyomi.domain.anime.repository

import tachiyomi.domain.anime.model.FavoriteEntry
import tachiyomi.domain.anime.model.FavoriteEntryAlternative

interface FavoritesEntryRepository {
    suspend fun deleteAll()

    suspend fun insertAll(favoriteEntries: List<FavoriteEntry>)

    suspend fun selectAll(): List<FavoriteEntry>

    suspend fun addAlternative(favoriteEntryAlternative: FavoriteEntryAlternative)
}
