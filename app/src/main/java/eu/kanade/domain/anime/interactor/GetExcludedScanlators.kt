package eu.kanade.domain.anime.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.data.DatabaseHandler

class GetExcludedScanlators(
    private val handler: DatabaseHandler,
) {

    suspend fun await(animeId: Long): Set<String> {
        return handler.awaitList {
            excluded_scanlatorsQueries.getExcludedScanlatorsByMangaId(animeId)
        }
            .toSet()
    }

    fun subscribe(animeId: Long): Flow<Set<String>> {
        return handler.subscribeToList {
            excluded_scanlatorsQueries.getExcludedScanlatorsByMangaId(animeId)
        }
            .map { it.toSet() }
    }
}
