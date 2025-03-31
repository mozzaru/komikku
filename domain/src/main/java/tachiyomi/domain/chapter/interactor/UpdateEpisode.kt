package tachiyomi.domain.chapter.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.EpisodeUpdate
import tachiyomi.domain.chapter.repository.ChapterRepository

class UpdateEpisode(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(episodeUpdate: EpisodeUpdate) {
        try {
            chapterRepository.update(episodeUpdate)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    suspend fun awaitAll(episodeUpdates: List<EpisodeUpdate>) {
        try {
            chapterRepository.updateAll(episodeUpdates)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
