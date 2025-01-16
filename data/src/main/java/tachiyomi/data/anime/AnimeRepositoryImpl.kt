package tachiyomi.data.anime

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.AndroidDatabaseHandler
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.data.UpdateStrategyColumnAdapter
import tachiyomi.domain.anime.model.AnimeUpdate
import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.anime.repository.AnimeRepository
import tachiyomi.domain.library.model.LibraryAnime
import java.time.LocalDate
import java.time.ZoneId

class AnimeRepositoryImpl(
    private val handler: DatabaseHandler,
) : AnimeRepository {

    override suspend fun getMangaById(id: Long): Manga {
        return handler.awaitOne { mangasQueries.getMangaById(id, AnimeMapper::mapAnime) }
    }

    override suspend fun getMangaByIdAsFlow(id: Long): Flow<Manga> {
        return handler.subscribeToOne { mangasQueries.getMangaById(id, AnimeMapper::mapAnime) }
    }

    override suspend fun getMangaByUrlAndSourceId(url: String, sourceId: Long): Manga? {
        return handler.awaitOneOrNull {
            mangasQueries.getMangaByUrlAndSource(
                url,
                sourceId,
                AnimeMapper::mapAnime,
            )
        }
    }

    override fun getMangaByUrlAndSourceIdAsFlow(url: String, sourceId: Long): Flow<Manga?> {
        return handler.subscribeToOneOrNull {
            mangasQueries.getMangaByUrlAndSource(
                url,
                sourceId,
                AnimeMapper::mapAnime,
            )
        }
    }

    override suspend fun getFavorites(): List<Manga> {
        return handler.awaitList { mangasQueries.getFavorites(AnimeMapper::mapAnime) }
    }

    override suspend fun getReadMangaNotInLibrary(): List<Manga> {
        return handler.awaitList { mangasQueries.getReadMangaNotInLibrary(AnimeMapper::mapAnime) }
    }

    override suspend fun getLibraryManga(): List<LibraryAnime> {
        return handler.awaitListExecutable {
            (handler as AndroidDatabaseHandler).getLibraryQuery()
        }.map(AnimeMapper::mapLibraryView)
        // return handler.awaitList { libraryViewQueries.library(AnimeMapper::mapLibraryAnime) }
    }

    override fun getLibraryMangaAsFlow(): Flow<List<LibraryAnime>> {
        return handler.subscribeToList { libraryViewQueries.library(AnimeMapper::mapLibraryAnime) }
            // SY -->
            .map { getLibraryManga() }
        // SY <--
    }

    override fun getFavoritesBySourceId(sourceId: Long): Flow<List<Manga>> {
        return handler.subscribeToList { mangasQueries.getFavoriteBySourceId(sourceId, AnimeMapper::mapAnime) }
    }

    override suspend fun getDuplicateLibraryManga(id: Long, title: String): List<Manga> {
        return handler.awaitList {
            mangasQueries.getDuplicateLibraryManga(title, id, AnimeMapper::mapAnime)
        }
    }

    override suspend fun getUpcomingManga(statuses: Set<Long>): Flow<List<Manga>> {
        val epochMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        return handler.subscribeToList {
            mangasQueries.getUpcomingManga(epochMillis, statuses, AnimeMapper::mapAnime)
        }
    }

    override suspend fun resetViewerFlags(): Boolean {
        return try {
            handler.await { mangasQueries.resetViewerFlags() }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun setMangaCategories(mangaId: Long, categoryIds: List<Long>) {
        handler.await(inTransaction = true) {
            mangas_categoriesQueries.deleteMangaCategoryByMangaId(mangaId)
            categoryIds.map { categoryId ->
                mangas_categoriesQueries.insert(mangaId, categoryId)
            }
        }
    }

    override suspend fun insert(manga: Manga): Long? {
        return handler.awaitOneOrNullExecutable(inTransaction = true) {
            // SY -->
            if (mangasQueries.getIdByUrlAndSource(manga.url, manga.source).executeAsOneOrNull() != null) {
                return@awaitOneOrNullExecutable mangasQueries.getIdByUrlAndSource(manga.url, manga.source)
            }
            // SY <--
            mangasQueries.insert(
                source = manga.source,
                url = manga.url,
                artist = manga.artist,
                author = manga.author,
                description = manga.description,
                genre = manga.genre,
                title = manga.title,
                status = manga.status,
                thumbnailUrl = manga.thumbnailUrl,
                favorite = manga.favorite,
                lastUpdate = manga.lastUpdate,
                nextUpdate = manga.nextUpdate,
                calculateInterval = manga.fetchInterval.toLong(),
                initialized = manga.initialized,
                viewerFlags = manga.viewerFlags,
                chapterFlags = manga.chapterFlags,
                coverLastModified = manga.coverLastModified,
                dateAdded = manga.dateAdded,
                updateStrategy = manga.updateStrategy,
                version = manga.version,
            )
            mangasQueries.selectLastInsertedRowId()
        }
    }

    override suspend fun update(update: AnimeUpdate): Boolean {
        return try {
            partialUpdate(update)
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun updateAll(mangaUpdates: List<AnimeUpdate>): Boolean {
        return try {
            partialUpdate(*mangaUpdates.toTypedArray())
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    private suspend fun partialUpdate(vararg mangaUpdates: AnimeUpdate) {
        handler.await(inTransaction = true) {
            mangaUpdates.forEach { value ->
                mangasQueries.update(
                    source = value.source,
                    url = value.url,
                    artist = value.artist,
                    author = value.author,
                    description = value.description,
                    genre = value.genre?.let(StringListColumnAdapter::encode),
                    title = value.title,
                    status = value.status,
                    thumbnailUrl = value.thumbnailUrl,
                    favorite = value.favorite,
                    lastUpdate = value.lastUpdate,
                    nextUpdate = value.nextUpdate,
                    calculateInterval = value.fetchInterval?.toLong(),
                    initialized = value.initialized,
                    viewer = value.viewerFlags,
                    chapterFlags = value.chapterFlags,
                    coverLastModified = value.coverLastModified,
                    dateAdded = value.dateAdded,
                    mangaId = value.id,
                    updateStrategy = value.updateStrategy?.let(UpdateStrategyColumnAdapter::encode),
                    version = value.version,
                    isSyncing = 0,
                )
            }
        }
    }

    // SY -->
    override suspend fun getMangaBySourceId(sourceId: Long): List<Manga> {
        return handler.awaitList { mangasQueries.getBySource(sourceId, AnimeMapper::mapAnime) }
    }

    override suspend fun getAll(): List<Manga> {
        return handler.awaitList { mangasQueries.getAll(AnimeMapper::mapAnime) }
    }

    override suspend fun deleteManga(mangaId: Long) {
        handler.await { mangasQueries.deleteById(mangaId) }
    }

    override suspend fun getReadMangaNotInLibraryView(): List<LibraryAnime> {
        return handler.awaitListExecutable {
            (handler as AndroidDatabaseHandler).getLibraryQuery("M.favorite = 0 AND C.readCount != 0")
        }.map(AnimeMapper::mapLibraryView)
    }
    // SY <--
}
