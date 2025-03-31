package eu.kanade.domain.episode.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.chapter.repository.ChapterRepository

class GetAvailableScanlators(
    private val repository: ChapterRepository,
) {

    private fun List<String>.cleanupAvailableScanlators(): Set<String> {
        return mapNotNull { it.ifBlank { null } }.toSet()
    }

    suspend fun await(animeId: Long): Set<String> {
        return repository.getScanlatorsByMangaId(animeId)
            .cleanupAvailableScanlators()
    }

    fun subscribe(animeId: Long): Flow<Set<String>> {
        return repository.getScanlatorsByMangaIdAsFlow(animeId)
            .map { it.cleanupAvailableScanlators() }
    }

    // SY -->
    suspend fun awaitMerge(animeId: Long): Set<String> {
        return repository.getScanlatorsByMergeId(animeId)
            .cleanupAvailableScanlators()
    }

    fun subscribeMerge(animeId: Long): Flow<Set<String>> {
        return repository.getScanlatorsByMergeIdAsFlow(animeId)
            .map { it.cleanupAvailableScanlators() }
    }
    // SY <--
}
