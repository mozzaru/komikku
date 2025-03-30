package tachiyomi.domain.manga.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.library.model.LibraryManga

interface MangaRepository {

    suspend fun getMangaById(id: Long): Manga

    suspend fun getAnimeByIdAsFlow(id: Long): Flow<Manga>

    suspend fun getAnimeByUrlAndSourceId(url: String, sourceId: Long): Manga?

    fun getAnimeByUrlAndSourceIdAsFlow(url: String, sourceId: Long): Flow<Manga?>

    suspend fun getFavorites(): List<Manga>

    suspend fun getSeenAnimeNotInLibrary(): List<Manga>

    suspend fun getLibraryManga(): List<LibraryManga>

    fun getLibraryMangaAsFlow(): Flow<List<LibraryManga>>

    fun getFavoritesBySourceId(sourceId: Long): Flow<List<Manga>>

    suspend fun getDuplicateLibraryAnime(id: Long, title: String): List<Manga>

    suspend fun getUpcomingManga(statuses: Set<Long>): Flow<List<Manga>>

    suspend fun resetViewerFlags(): Boolean

    suspend fun setMangaCategories(mangaId: Long, categoryIds: List<Long>)

    suspend fun insert(manga: Manga): Long?

    suspend fun update(update: MangaUpdate): Boolean

    suspend fun updateAll(mangaUpdates: List<MangaUpdate>): Boolean

    // SY -->
    suspend fun getMangaBySourceId(sourceId: Long): List<Manga>

    suspend fun getAll(): List<Manga>

    suspend fun deleteManga(mangaId: Long)

    suspend fun getReadMangaNotInLibraryView(): List<LibraryManga>
    // SY <--
}
