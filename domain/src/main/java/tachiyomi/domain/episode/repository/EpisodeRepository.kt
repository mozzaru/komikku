package tachiyomi.domain.episode.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.episode.model.EpisodeUpdate

interface EpisodeRepository {

    suspend fun addAll(episodes: List<Episode>): List<Episode>

    suspend fun update(episodeUpdate: EpisodeUpdate)

    suspend fun updateAll(episodeUpdates: List<EpisodeUpdate>)

    suspend fun removeChaptersWithIds(chapterIds: List<Long>)

    suspend fun getChapterByMangaId(mangaId: Long, applyScanlatorFilter: Boolean = false): List<Episode>

    suspend fun getScanlatorsByMangaId(mangaId: Long): List<String>

    fun getScanlatorsByMangaIdAsFlow(mangaId: Long): Flow<List<String>>

    suspend fun getBookmarkedChaptersByMangaId(mangaId: Long): List<Episode>

    suspend fun getChapterById(id: Long): Episode?

    suspend fun getChapterByMangaIdAsFlow(mangaId: Long, applyScanlatorFilter: Boolean = false): Flow<List<Episode>>

    suspend fun getChapterByUrlAndMangaId(url: String, mangaId: Long): Episode?

    // SY -->
    suspend fun getChapterByUrl(url: String): List<Episode>

    suspend fun getMergedChapterByMangaId(mangaId: Long, applyScanlatorFilter: Boolean = false): List<Episode>

    suspend fun getMergedChapterByMangaIdAsFlow(
        mangaId: Long,
        applyScanlatorFilter: Boolean = false,
    ): Flow<List<Episode>>

    suspend fun getScanlatorsByMergeId(mangaId: Long): List<String>

    fun getScanlatorsByMergeIdAsFlow(mangaId: Long): Flow<List<String>>
    // SY <--
}
