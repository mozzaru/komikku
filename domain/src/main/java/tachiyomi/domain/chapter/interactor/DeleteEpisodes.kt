package tachiyomi.domain.chapter.interactor

import tachiyomi.domain.chapter.repository.EpisodeRepository

class DeleteEpisodes(
    private val episodeRepository: EpisodeRepository,
) {

    suspend fun await(chapters: List<Long>) {
        episodeRepository.removeEpisodesWithIds(chapters)
    }
}
