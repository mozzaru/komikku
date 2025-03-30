package tachiyomi.domain.chapter.interactor

import tachiyomi.domain.chapter.model.Episode
import tachiyomi.domain.chapter.repository.EpisodeRepository

class GetEpisodeByUrlAndAnimeId(
    private val episodeRepository: EpisodeRepository,
) {

    suspend fun await(url: String, sourceId: Long): Episode? {
        return try {
            episodeRepository.getEpisodeByUrlAndAnimeId(url, sourceId)
        } catch (e: Exception) {
            null
        }
    }
}
