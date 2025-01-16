package tachiyomi.domain.episode.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.episode.model.Chapter
import tachiyomi.domain.episode.repository.EpisodeRepository

class GetEpisodeByUrl(
    private val episodeRepository: EpisodeRepository,
) {

    suspend fun await(url: String): List<Chapter> {
        return try {
            episodeRepository.getChapterByUrl(url)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }
}
