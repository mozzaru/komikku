package tachiyomi.domain.manga.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.chapter.model.Episode
import tachiyomi.domain.chapter.repository.ChapterRepository

class GetAnimeWithEpisodes(
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
) {

    suspend fun subscribe(id: Long, applyScanlatorFilter: Boolean = false): Flow<Pair<Manga, List<Episode>>> {
        return combine(
            mangaRepository.getAnimeByIdAsFlow(id),
            chapterRepository.getEpisodeByAnimeIdAsFlow(id, applyScanlatorFilter),
        ) { manga, chapters ->
            Pair(manga, chapters)
        }
    }

    suspend fun awaitManga(id: Long): Manga {
        return mangaRepository.getMangaById(id)
    }

    suspend fun awaitChapters(id: Long, applyScanlatorFilter: Boolean = false): List<Episode> {
        return chapterRepository.getEpisodeByAnimeId(id, applyScanlatorFilter)
    }
}
