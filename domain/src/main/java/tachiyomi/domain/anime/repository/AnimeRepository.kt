package tachiyomi.domain.anime.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.anime.model.AnimeUpdate
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.library.model.LibraryAnime

interface AnimeRepository {

    suspend fun getMangaById(id: Long): Anime

    suspend fun getMangaByIdAsFlow(id: Long): Flow<Anime>

    suspend fun getMangaByUrlAndSourceId(url: String, sourceId: Long): Anime?

    fun getMangaByUrlAndSourceIdAsFlow(url: String, sourceId: Long): Flow<Anime?>

    suspend fun getFavorites(): List<Anime>

    suspend fun getReadMangaNotInLibrary(): List<Anime>

    suspend fun getLibraryManga(): List<LibraryAnime>

    fun getLibraryMangaAsFlow(): Flow<List<LibraryAnime>>

    fun getFavoritesBySourceId(sourceId: Long): Flow<List<Anime>>

    suspend fun getDuplicateLibraryManga(id: Long, title: String): List<Anime>

    suspend fun getUpcomingManga(statuses: Set<Long>): Flow<List<Anime>>

    suspend fun resetViewerFlags(): Boolean

    suspend fun setMangaCategories(mangaId: Long, categoryIds: List<Long>)

    suspend fun insert(anime: Anime): Long?

    suspend fun update(update: AnimeUpdate): Boolean

    suspend fun updateAll(animeUpdates: List<AnimeUpdate>): Boolean

    // SY -->
    suspend fun getMangaBySourceId(sourceId: Long): List<Anime>

    suspend fun getAll(): List<Anime>

    suspend fun deleteManga(mangaId: Long)

    suspend fun getReadMangaNotInLibraryView(): List<LibraryAnime>
    // SY <--
}
