package tachiyomi.domain.anime.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.anime.repository.AnimeRepository
import tachiyomi.domain.episode.model.Chapter
import tachiyomi.domain.episode.repository.EpisodeRepository

class GetAnimeWithEpisodes(
    private val animeRepository: AnimeRepository,
    private val episodeRepository: EpisodeRepository,
) {

    suspend fun subscribe(id: Long, applyScanlatorFilter: Boolean = false): Flow<Pair<Manga, List<Chapter>>> {
        return combine(
            animeRepository.getMangaByIdAsFlow(id),
            episodeRepository.getChapterByMangaIdAsFlow(id, applyScanlatorFilter),
        ) { manga, chapters ->
            Pair(manga, chapters)
        }
    }

    suspend fun awaitManga(id: Long): Manga {
        return animeRepository.getMangaById(id)
    }

    suspend fun awaitChapters(id: Long, applyScanlatorFilter: Boolean = false): List<Chapter> {
        return episodeRepository.getChapterByMangaId(id, applyScanlatorFilter)
    }
}
