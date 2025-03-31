package tachiyomi.domain.chapter.interactor

import tachiyomi.domain.chapter.model.Episode
import tachiyomi.domain.chapter.repository.ChapterRepository

class GetEpisodeByUrlAndAnimeId(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(url: String, sourceId: Long): Episode? {
        return try {
            chapterRepository.getEpisodeByUrlAndAnimeId(url, sourceId)
        } catch (e: Exception) {
            null
        }
    }
}
