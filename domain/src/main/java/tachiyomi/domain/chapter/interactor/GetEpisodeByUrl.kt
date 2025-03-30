package tachiyomi.domain.chapter.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Episode
import tachiyomi.domain.chapter.repository.EpisodeRepository

class GetEpisodeByUrl(
    private val episodeRepository: EpisodeRepository,
) {

    suspend fun await(url: String): List<Episode> {
        return try {
            episodeRepository.getEpisodeByUrl(url)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }
}
