package tachiyomi.domain.episode.interactor

import tachiyomi.domain.episode.repository.EpisodeRepository

class DeleteEpisodes(
    private val episodeRepository: EpisodeRepository,
) {

    suspend fun await(episodes: List<Long>) {
        episodeRepository.removeEpisodesWithIds(episodes)
    }
}
