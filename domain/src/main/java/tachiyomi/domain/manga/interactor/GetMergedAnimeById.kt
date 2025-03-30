package tachiyomi.domain.manga.interactor

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.AnimeMergeRepository

class GetMergedAnimeById(
    private val animeMergeRepository: AnimeMergeRepository,
) {

    suspend fun await(id: Long): List<Manga> {
        return try {
            animeMergeRepository.getMergedAnimeById(id)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    suspend fun subscribe(id: Long): Flow<List<Manga>> {
        return animeMergeRepository.subscribeMergedAnimeById(id)
    }
}
