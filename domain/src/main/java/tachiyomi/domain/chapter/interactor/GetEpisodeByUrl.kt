package tachiyomi.domain.chapter.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Episode
import tachiyomi.domain.chapter.repository.ChapterRepository

class GetEpisodeByUrl(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(url: String): List<Episode> {
        return try {
            chapterRepository.getEpisodeByUrl(url)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }
}
