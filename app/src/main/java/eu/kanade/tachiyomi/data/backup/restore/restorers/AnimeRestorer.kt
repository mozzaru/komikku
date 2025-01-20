package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.tachiyomi.data.backup.models.BackupAnime
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupEpisode
import eu.kanade.tachiyomi.data.backup.models.BackupFlatMetadata
import eu.kanade.tachiyomi.data.backup.models.BackupHistory
import eu.kanade.tachiyomi.data.backup.models.BackupMergedMangaReference
import eu.kanade.tachiyomi.data.backup.models.BackupTracking
import exh.EXHMigrations
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.UpdateStrategyColumnAdapter
import tachiyomi.data.anime.AnimeMapper
import tachiyomi.data.anime.MergedAnimeMapper
import tachiyomi.domain.anime.interactor.FetchInterval
import tachiyomi.domain.anime.interactor.GetAnimeByUrlAndSourceId
import tachiyomi.domain.anime.interactor.GetFlatMetadataById
import tachiyomi.domain.anime.interactor.InsertFlatMetadata
import tachiyomi.domain.anime.interactor.SetCustomAnimeInfo
import tachiyomi.domain.anime.model.CustomAnimeInfo
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.track.model.Track
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.ZonedDateTime
import java.util.Date
import kotlin.math.max

class AnimeRestorer(
    private var isSync: Boolean = false,

    private val handler: DatabaseHandler = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val getAnimeByUrlAndSourceId: GetAnimeByUrlAndSourceId = Injekt.get(),
    private val getEpisodesByAnimeId: GetEpisodesByAnimeId = Injekt.get(),
    private val updateAnime: UpdateAnime = Injekt.get(),
    private val getTracks: GetTracks = Injekt.get(),
    private val insertTrack: InsertTrack = Injekt.get(),
    fetchInterval: FetchInterval = Injekt.get(),
    // SY -->
    private val setCustomAnimeInfo: SetCustomAnimeInfo = Injekt.get(),
    private val insertFlatMetadata: InsertFlatMetadata = Injekt.get(),
    private val getFlatMetadataById: GetFlatMetadataById = Injekt.get(),
    // SY <--
) {
    private var now = ZonedDateTime.now()
    private var currentFetchWindow = fetchInterval.getWindow(now)

    init {
        now = ZonedDateTime.now()
        currentFetchWindow = fetchInterval.getWindow(now)
    }

    suspend fun sortByNew(backupAnimes: List<BackupAnime>): List<BackupAnime> {
        val urlsBySource = handler.awaitList { animesQueries.getAllAnimeSourceAndUrl() }
            .groupBy({ it.source }, { it.url })

        return backupAnimes
            .sortedWith(
                compareBy<BackupAnime> { it.url in urlsBySource[it.source].orEmpty() }
                    .then(compareByDescending { it.lastModifiedAt }),
            )
    }

    /**
     * Restore a single anime
     */
    suspend fun restore(
        backupAnime: BackupAnime,
        backupCategories: List<BackupCategory>,
    ) {
        handler.await(inTransaction = true) {
            val dbManga = findExistingManga(backupAnime)
            var manga = backupAnime.getMangaImpl()
            // SY -->
            manga = EXHMigrations.migrateBackupEntry(manga)
            // SY <--
            val restoredManga = if (dbManga == null) {
                restoreNewManga(manga)
            } else {
                restoreExistingManga(manga, dbManga)
            }

            restoreMangaDetails(
                anime = restoredManga,
                chapters = backupAnime.chapters,
                categories = backupAnime.categories,
                backupCategories = backupCategories,
                history = backupAnime.history,
                tracks = backupAnime.tracking,
                excludedScanlators = backupAnime.excludedScanlators,
                // SY -->
                mergedMangaReferences = backupAnime.mergedMangaReferences,
                flatMetadata = backupAnime.flatMetadata,
                customManga = backupAnime.getCustomMangaInfo(),
                // SY <--
            )

            if (isSync) {
                animesQueries.resetIsSyncing()
                episodesQueries.resetIsSyncing()
            }
        }
    }

    private suspend fun findExistingManga(backupAnime: BackupAnime): Anime? {
        return getAnimeByUrlAndSourceId.await(backupAnime.url, backupAnime.source)
    }

    private suspend fun restoreExistingManga(anime: Anime, dbAnime: Anime): Anime {
        return if (anime.version > dbAnime.version) {
            updateManga(dbAnime.copyFrom(anime).copy(id = dbAnime.id))
        } else {
            updateManga(anime.copyFrom(dbAnime).copy(id = dbAnime.id))
        }
    }

    private fun Anime.copyFrom(newer: Anime): Anime {
        return this.copy(
            favorite = this.favorite || newer.favorite,
            // SY -->
            ogAuthor = newer.author,
            ogArtist = newer.artist,
            ogDescription = newer.description,
            ogGenre = newer.genre,
            ogThumbnailUrl = newer.thumbnailUrl,
            ogStatus = newer.status,
            // SY <--
            initialized = this.initialized || newer.initialized,
            version = newer.version,
        )
    }

    suspend fun updateManga(anime: Anime): Anime {
        handler.await(true) {
            animesQueries.update(
                source = anime.source,
                url = anime.url,
                artist = anime.artist,
                author = anime.author,
                description = anime.description,
                genre = anime.genre?.joinToString(separator = ", "),
                title = anime.title,
                status = anime.status,
                thumbnailUrl = anime.thumbnailUrl,
                favorite = anime.favorite,
                lastUpdate = anime.lastUpdate,
                nextUpdate = null,
                calculateInterval = null,
                initialized = anime.initialized,
                viewer = anime.viewerFlags,
                chapterFlags = anime.chapterFlags,
                coverLastModified = anime.coverLastModified,
                dateAdded = anime.dateAdded,
                mangaId = anime.id,
                updateStrategy = anime.updateStrategy.let(UpdateStrategyColumnAdapter::encode),
                version = anime.version,
                isSyncing = 1,
            )
        }
        return anime
    }

    private suspend fun restoreNewManga(
        anime: Anime,
    ): Anime {
        return anime.copy(
            initialized = anime.description != null,
            id = insertManga(anime),
            version = anime.version,
        )
    }

    private suspend fun restoreChapters(anime: Anime, backupEpisodes: List<BackupEpisode>) {
        val dbChaptersByUrl = getEpisodesByAnimeId.await(anime.id)
            .associateBy { it.url }

        val (existingChapters, newChapters) = backupEpisodes
            .mapNotNull { backupChapter ->
                val chapter = backupChapter.toChapterImpl().copy(mangaId = anime.id)
                val dbChapter = dbChaptersByUrl[chapter.url]

                when {
                    dbChapter == null -> chapter // New episode
                    chapter.forComparison() == dbChapter.forComparison() -> null // Same state; skip
                    else -> updateChapterBasedOnSyncState(chapter, dbChapter) // Update existed episode
                }
            }
            .partition { it.id > 0 }

        insertNewChapters(newChapters)
        updateExistingChapters(existingChapters)
    }

    private fun updateChapterBasedOnSyncState(episode: Episode, dbEpisode: Episode): Episode {
        return if (isSync) {
            episode.copy(
                id = dbEpisode.id,
                bookmark = episode.bookmark || dbEpisode.bookmark,
                read = episode.read,
                lastPageRead = episode.lastPageRead,
                sourceOrder = episode.sourceOrder,
            )
        } else {
            episode.copyFrom(dbEpisode).let {
                when {
                    dbEpisode.read && !it.read -> it.copy(read = true, lastPageRead = dbEpisode.lastPageRead)
                    it.lastPageRead == 0L && dbEpisode.lastPageRead != 0L -> it.copy(
                        lastPageRead = dbEpisode.lastPageRead,
                    )
                    else -> it
                }
            }
                // KMK -->
                .copy(id = dbEpisode.id)
            // KMK <--
        }
    }

    private fun Episode.forComparison() =
        this.copy(
            id = 0L,
            mangaId = 0L,
            dateFetch = 0L,
            // KMK -->
            // dateUpload = 0L, some time source loses dateUpload so we overwrite with backup
            sourceOrder = 0L, // ignore sourceOrder since it will be updated on refresh
            // KMK <--
            lastModifiedAt = 0L,
            version = 0L,
        )

    private suspend fun insertNewChapters(episodes: List<Episode>) {
        handler.await(true) {
            episodes.forEach { chapter ->
                episodesQueries.insert(
                    chapter.mangaId,
                    chapter.url,
                    chapter.name,
                    chapter.scanlator,
                    chapter.read,
                    chapter.bookmark,
                    chapter.lastPageRead,
                    chapter.chapterNumber,
                    chapter.sourceOrder,
                    chapter.dateFetch,
                    chapter.dateUpload,
                    chapter.version,
                )
            }
        }
    }

    private suspend fun updateExistingChapters(episodes: List<Episode>) {
        handler.await(true) {
            episodes.forEach { chapter ->
                episodesQueries.update(
                    mangaId = null,
                    url = null,
                    name = null,
                    scanlator = null,
                    read = chapter.read,
                    bookmark = chapter.bookmark,
                    lastPageRead = chapter.lastPageRead,
                    chapterNumber = null,
                    sourceOrder = if (isSync) chapter.sourceOrder else null,
                    dateFetch = null,
                    // KMK -->
                    dateUpload = chapter.dateUpload,
                    // KMK <--
                    chapterId = chapter.id,
                    version = chapter.version,
                    isSyncing = 1,
                )
            }
        }
    }

    /**
     * Inserts anime and returns id
     *
     * @return id of [Anime], null if not found
     */
    private suspend fun insertManga(anime: Anime): Long {
        return handler.awaitOneExecutable(true) {
            animesQueries.insert(
                source = anime.source,
                url = anime.url,
                artist = anime.artist,
                author = anime.author,
                description = anime.description,
                genre = anime.genre,
                title = anime.title,
                status = anime.status,
                thumbnailUrl = anime.thumbnailUrl,
                favorite = anime.favorite,
                lastUpdate = anime.lastUpdate,
                nextUpdate = 0L,
                calculateInterval = 0L,
                initialized = anime.initialized,
                viewerFlags = anime.viewerFlags,
                chapterFlags = anime.chapterFlags,
                coverLastModified = anime.coverLastModified,
                dateAdded = anime.dateAdded,
                updateStrategy = anime.updateStrategy,
                version = anime.version,
            )
            animesQueries.selectLastInsertedRowId()
        }
    }

    private suspend fun restoreMangaDetails(
        anime: Anime,
        chapters: List<BackupEpisode>,
        categories: List<Long>,
        backupCategories: List<BackupCategory>,
        history: List<BackupHistory>,
        tracks: List<BackupTracking>,
        excludedScanlators: List<String>,
        // SY -->
        mergedMangaReferences: List<BackupMergedMangaReference>,
        flatMetadata: BackupFlatMetadata?,
        customManga: CustomAnimeInfo?,
        // SY <--
    ): Anime {
        restoreCategories(anime, categories, backupCategories)
        restoreChapters(anime, chapters)
        restoreTracking(anime, tracks)
        restoreHistory(anime, history)
        restoreExcludedScanlators(anime, excludedScanlators)
        updateAnime.awaitUpdateFetchInterval(anime, now, currentFetchWindow)
        // SY -->
        restoreMergedMangaReferencesForManga(anime.id, mergedMangaReferences)
        flatMetadata?.let { restoreFlatMetadata(anime.id, it) }
        restoreEditedInfo(customManga?.copy(id = anime.id))
        // SY <--

        return anime
    }

    /**
     * Restores the categories a anime is in.
     *
     * @param anime the anime whose categories have to be restored.
     * @param categories the categories to restore.
     */
    private suspend fun restoreCategories(
        anime: Anime,
        categories: List<Long>,
        backupCategories: List<BackupCategory>,
    ) {
        val dbCategories = getCategories.await()
        val dbCategoriesByName = dbCategories.associateBy { it.name }

        val backupCategoriesByOrder = backupCategories.associateBy { it.order }

        val mangaCategoriesToUpdate = categories.mapNotNull { backupCategoryOrder ->
            backupCategoriesByOrder[backupCategoryOrder]?.let { backupCategory ->
                dbCategoriesByName[backupCategory.name]?.let { dbCategory ->
                    Pair(anime.id, dbCategory.id)
                }
            }
        }

        if (mangaCategoriesToUpdate.isNotEmpty()) {
            handler.await(true) {
                animes_categoriesQueries.deleteAnimeCategoryByAnimeId(anime.id)
                mangaCategoriesToUpdate.forEach { (mangaId, categoryId) ->
                    animes_categoriesQueries.insert(mangaId, categoryId)
                }
            }
        }
    }

    private suspend fun restoreHistory(anime: Anime, backupHistory: List<BackupHistory>) {
        val toUpdate = backupHistory.mapNotNull { history ->
            val dbHistory = handler.awaitOneOrNull { historyQueries.getHistoryByEpisodeUrl(anime.id, history.url) }
            val item = history.getHistoryImpl()

            if (dbHistory == null) {
                val chapter = handler.awaitList { episodesQueries.getEpisodeByUrl(history.url) }
                    .find { it.manga_id == anime.id }
                return@mapNotNull if (chapter == null) {
                    // Episode doesn't exist; skip
                    null
                } else {
                    // New history entry
                    item.copy(chapterId = chapter._id)
                }
            }

            // Update history entry
            item.copy(
                id = dbHistory._id,
                chapterId = dbHistory.chapter_id,
                readAt = max(item.readAt?.time ?: 0L, dbHistory.last_read?.time ?: 0L)
                    .takeIf { it > 0L }
                    ?.let { Date(it) },
                readDuration = max(item.readDuration, dbHistory.time_read) - dbHistory.time_read,
            )
        }

        if (toUpdate.isNotEmpty()) {
            handler.await(true) {
                toUpdate.forEach {
                    historyQueries.upsert(
                        it.chapterId,
                        it.readAt,
                        it.readDuration,
                    )
                }
            }
        }
    }

    private suspend fun restoreTracking(anime: Anime, backupTracks: List<BackupTracking>) {
        val dbTrackByTrackerId = getTracks.await(anime.id).associateBy { it.trackerId }

        val (existingTracks, newTracks) = backupTracks
            .mapNotNull {
                val track = it.getTrackImpl()
                val dbTrack = dbTrackByTrackerId[track.trackerId]
                    ?: // New track
                    return@mapNotNull track.copy(
                        id = 0, // Let DB assign new ID
                        mangaId = anime.id,
                    )

                if (track.forComparison() == dbTrack.forComparison()) {
                    // Same state; skip
                    return@mapNotNull null
                }

                // Update to an existing track
                dbTrack.copy(
                    remoteId = track.remoteId,
                    libraryId = track.libraryId,
                    lastChapterRead = max(dbTrack.lastChapterRead, track.lastChapterRead),
                )
            }
            .partition { it.id > 0 }

        if (newTracks.isNotEmpty()) {
            insertTrack.awaitAll(newTracks)
        }
        if (existingTracks.isNotEmpty()) {
            handler.await(true) {
                existingTracks.forEach { track ->
                    anime_syncQueries.update(
                        track.mangaId,
                        track.trackerId,
                        track.remoteId,
                        track.libraryId,
                        track.title,
                        track.lastChapterRead,
                        track.totalChapters,
                        track.status,
                        track.score,
                        track.remoteUrl,
                        track.startDate,
                        track.finishDate,
                        track.id,
                    )
                }
            }
        }
    }

    // SY -->
    /**
     * Restore the categories from Json
     *
     * @param mergeMangaId the merge anime for the references
     * @param backupMergedMangaReferences the list of backup anime references for the merged anime
     */
    private suspend fun restoreMergedMangaReferencesForManga(
        mergeMangaId: Long,
        backupMergedMangaReferences: List<BackupMergedMangaReference>,
    ) {
        // Get merged anime references from file and from db
        val dbMergedMangaReferences = handler.awaitList {
            mergedQueries.selectAll(MergedAnimeMapper::map)
        }

        // Iterate over them
        backupMergedMangaReferences.forEach { backupMergedMangaReference ->
            // If the backupMergedMangaReference isn't in the db,
            // remove the id and insert a new backupMergedMangaReference
            // Store the inserted id in the backupMergedMangaReference
            if (dbMergedMangaReferences.none {
                    backupMergedMangaReference.mergeUrl == it.mergeUrl &&
                        backupMergedMangaReference.mangaUrl == it.mangaUrl
                }
            ) {
                // Let the db assign the id
                val mergedManga = handler.awaitOneOrNull {
                    animesQueries.getAnimeByUrlAndSource(
                        backupMergedMangaReference.mangaUrl,
                        backupMergedMangaReference.mangaSourceId,
                        AnimeMapper::mapAnime,
                    )
                } ?: return@forEach
                backupMergedMangaReference.getMergedMangaReference().run {
                    handler.await {
                        mergedQueries.insert(
                            infoManga = isInfoManga,
                            getChapterUpdates = getChapterUpdates,
                            chapterSortMode = chapterSortMode.toLong(),
                            chapterPriority = chapterPriority.toLong(),
                            downloadChapters = downloadChapters,
                            mergeId = mergeMangaId,
                            mergeUrl = mergeUrl,
                            mangaId = mergedManga.id,
                            mangaUrl = mangaUrl,
                            mangaSource = mangaSourceId,
                        )
                    }
                }
            }
        }
    }

    private suspend fun restoreFlatMetadata(mangaId: Long, backupFlatMetadata: BackupFlatMetadata) {
        if (getFlatMetadataById.await(mangaId) == null) {
            insertFlatMetadata.await(backupFlatMetadata.getFlatMetadata(mangaId))
        }
    }

    private fun restoreEditedInfo(mangaJson: CustomAnimeInfo?) {
        mangaJson ?: return
        setCustomAnimeInfo.set(mangaJson)
    }

    fun BackupAnime.getCustomMangaInfo(): CustomAnimeInfo? {
        if (customTitle != null ||
            customArtist != null ||
            customAuthor != null ||
            customThumbnailUrl != null ||
            customDescription != null ||
            customGenre != null ||
            customStatus != 0
        ) {
            return CustomAnimeInfo(
                id = 0L,
                title = customTitle,
                author = customAuthor,
                artist = customArtist,
                thumbnailUrl = customThumbnailUrl,
                description = customDescription,
                genre = customGenre,
                status = customStatus.takeUnless { it == 0 }?.toLong(),
            )
        }
        return null
    }
    // SY <--

    private fun Track.forComparison() = this.copy(id = 0L, mangaId = 0L)

    /**
     * Restores the excluded scanlators for the anime.
     *
     * @param anime the anime whose excluded scanlators have to be restored.
     * @param excludedScanlators the excluded scanlators to restore.
     */
    private suspend fun restoreExcludedScanlators(anime: Anime, excludedScanlators: List<String>) {
        if (excludedScanlators.isEmpty()) return
        val existingExcludedScanlators = handler.awaitList {
            excluded_scanlatorsQueries.getExcludedScanlatorsByMangaId(anime.id)
        }
        val toInsert = excludedScanlators.filter { it !in existingExcludedScanlators }
        if (toInsert.isNotEmpty()) {
            handler.await {
                toInsert.forEach {
                    excluded_scanlatorsQueries.insert(anime.id, it)
                }
            }
        }
    }
}
