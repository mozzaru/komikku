package eu.kanade.tachiyomi.ui.browse.migration.advanced.process

import android.content.Context
import android.widget.Toast
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.domain.anime.model.hasCustomCover
import eu.kanade.domain.anime.model.toSAnime
import eu.kanade.domain.episode.interactor.SyncEpisodesWithSource
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.getNameForAnimeInfo
import eu.kanade.tachiyomi.source.online.all.EHentai
import eu.kanade.tachiyomi.ui.browse.migration.MigrationFlags
import eu.kanade.tachiyomi.ui.browse.migration.advanced.design.MigrationType
import eu.kanade.tachiyomi.ui.browse.migration.advanced.process.MigratingAnime.SearchResult
import eu.kanade.tachiyomi.util.system.toast
import exh.eh.EHentaiThrottleManager
import exh.smartsearch.SmartSearchEngine
import exh.source.MERGED_SOURCE_ID
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.UnsortedPreferences
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.anime.interactor.GetMergedReferencesById
import tachiyomi.domain.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.AnimeUpdate
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetAnimeCategories
import tachiyomi.domain.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.episode.interactor.UpdateEpisode
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.episode.model.EpisodeUpdate
import tachiyomi.domain.history.interactor.GetHistoryByAnimeId
import tachiyomi.domain.history.interactor.UpsertHistory
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.DeleteTrack
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.atomic.AtomicInteger

class MigrationListScreenModel(
    private val config: MigrationProcedureConfig,
    private val preferences: UnsortedPreferences = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val coverCache: CoverCache = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
    private val updateAnime: UpdateAnime = Injekt.get(),
    private val syncEpisodesWithSource: SyncEpisodesWithSource = Injekt.get(),
    private val updateEpisode: UpdateEpisode = Injekt.get(),
    private val getEpisodesByAnimeId: GetEpisodesByAnimeId = Injekt.get(),
    private val getMergedReferencesById: GetMergedReferencesById = Injekt.get(),
    private val getHistoryByAnimeId: GetHistoryByAnimeId = Injekt.get(),
    private val upsertHistory: UpsertHistory = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val setAnimeCategories: SetAnimeCategories = Injekt.get(),
    private val getTracks: GetTracks = Injekt.get(),
    private val insertTrack: InsertTrack = Injekt.get(),
    private val deleteTrack: DeleteTrack = Injekt.get(),
) : ScreenModel {

    private val smartSearchEngine = SmartSearchEngine(config.extraSearchParams)
    private val throttleManager = EHentaiThrottleManager()

    val migratingItems = MutableStateFlow<ImmutableList<MigratingAnime>?>(null)
    val migrationDone = MutableStateFlow(false)
    val finishedCount = MutableStateFlow(0)

    val manualMigrations = MutableStateFlow(0)

    val hideNotFound = preferences.hideNotFoundMigration().get()
    val showOnlyUpdates = preferences.showOnlyUpdatesMigration().get()

    val navigateOut = MutableSharedFlow<Unit>()

    val dialog = MutableStateFlow<Dialog?>(null)

    val migratingProgress = MutableStateFlow(Float.MAX_VALUE)

    private var migrateJob: Job? = null

    init {
        screenModelScope.launchIO {
            val animeIds = when (val migration = config.migration) {
                is MigrationType.AnimeList -> {
                    migration.animeIds
                }
                is MigrationType.AnimeSingle -> listOf(migration.fromAnimeId)
            }
            runMigrations(
                animeIds
                    .map {
                        async {
                            val anime = getAnime.await(it) ?: return@async null
                            MigratingAnime(
                                anime = anime,
                                episodeInfo = getEpisodeInfo(it),
                                sourcesString = sourceManager.getOrStub(anime.source).getNameForAnimeInfo(
                                    if (anime.source == MERGED_SOURCE_ID) {
                                        getMergedReferencesById.await(anime.id)
                                            .map { sourceManager.getOrStub(it.animeSourceId) }
                                    } else {
                                        null
                                    },
                                ),
                                parentContext = screenModelScope.coroutineContext,
                            )
                        }
                    }
                    .awaitAll()
                    .filterNotNull()
                    .also {
                        migratingItems.value = it.toImmutableList()
                    },
            )
        }
    }

    suspend fun getAnime(result: SearchResult.Result) = getAnime(result.id)
    suspend fun getAnime(id: Long) = getAnime.await(id)
    suspend fun getEpisodeInfo(result: SearchResult.Result) = getEpisodeInfo(result.id)
    suspend fun getEpisodeInfo(id: Long) = getEpisodesByAnimeId.await(id).let { episodes ->
        MigratingAnime.EpisodeInfo(
            latestEpisode = episodes.maxOfOrNull { it.episodeNumber },
            episodeCount = episodes.size,
        )
    }
    fun getSourceName(anime: Anime) = sourceManager.getOrStub(anime.source).getNameForAnimeInfo()

    fun getMigrationSources() = preferences.migrationSources().get().split("/").mapNotNull {
        val value = it.toLongOrNull() ?: return@mapNotNull null
        sourceManager.get(value) as? CatalogueSource
    }

    private suspend fun runMigrations(animes: List<MigratingAnime>) {
        throttleManager.resetThrottle()
        // KMK: finishedCount.value = animes.size
        val useSourceWithMost = preferences.useSourceWithMost().get()
        val useSmartSearch = preferences.smartMigration().get()

        val sources = getMigrationSources()
        for (anime in animes) {
            if (!currentCoroutineContext().isActive) {
                break
            }
            // in case it was removed
            when (val migration = config.migration) {
                is MigrationType.AnimeList -> if (anime.anime.id !in migration.animeIds) {
                    continue
                }
                else -> Unit
            }

            if (anime.searchResult.value == SearchResult.Searching && anime.migrationScope.isActive) {
                val animeObj = anime.anime
                val animeSource = sourceManager.getOrStub(animeObj.source)

                val result = try {
                    // KMK -->
                    anime.searchingJob = anime.migrationScope.async {
                        // KMK <--
                        val validSources = if (sources.size == 1) {
                            sources
                        } else {
                            sources.filter { it.id != animeSource.id }
                        }
                        when (val migration = config.migration) {
                            is MigrationType.AnimeSingle -> if (migration.toAnime != null) {
                                val localAnime = getAnime.await(migration.toAnime)
                                if (localAnime != null) {
                                    val source = sourceManager.get(localAnime.source) as? CatalogueSource
                                    if (source != null) {
                                        val episodes = if (source is EHentai) {
                                            source.getEpisodeList(localAnime.toSAnime(), throttleManager::throttle)
                                        } else {
                                            source.getEpisodeList(localAnime.toSAnime())
                                        }
                                        try {
                                            syncEpisodesWithSource.await(episodes, localAnime, source)
                                        } catch (_: Exception) {
                                        }
                                        anime.progress.value = validSources.size to validSources.size
                                        return@async localAnime
                                    }
                                }
                            }
                            else -> Unit
                        }
                        if (useSourceWithMost) {
                            val sourceSemaphore = Semaphore(3)
                            val processedSources = AtomicInteger()

                            validSources.map { source ->
                                async async2@{
                                    sourceSemaphore.withPermit {
                                        try {
                                            val searchResult = if (useSmartSearch) {
                                                smartSearchEngine.smartSearch(source, animeObj.ogTitle)
                                            } else {
                                                smartSearchEngine.normalSearch(source, animeObj.ogTitle)
                                            }

                                            if (searchResult != null &&
                                                !(searchResult.url == animeObj.url && source.id == animeObj.source)
                                            ) {
                                                val localAnime = networkToLocalAnime.await(searchResult)

                                                val episodes = if (source is EHentai) {
                                                    source.getEpisodeList(localAnime.toSAnime(), throttleManager::throttle)
                                                } else {
                                                    source.getEpisodeList(localAnime.toSAnime())
                                                }

                                                try {
                                                    syncEpisodesWithSource.await(episodes, localAnime, source)
                                                } catch (e: Exception) {
                                                    return@async2 null
                                                }
                                                anime.progress.value =
                                                    validSources.size to processedSources.incrementAndGet()
                                                localAnime to episodes.size
                                            } else {
                                                null
                                            }
                                        } catch (e: CancellationException) {
                                            // Ignore cancellations
                                            throw e
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                }
                            }.mapNotNull { it.await() }.maxByOrNull { it.second }?.first
                        } else {
                            validSources.forEachIndexed { index, source ->
                                val searchResult = try {
                                    val searchResult = if (useSmartSearch) {
                                        smartSearchEngine.smartSearch(source, animeObj.ogTitle)
                                    } else {
                                        smartSearchEngine.normalSearch(source, animeObj.ogTitle)
                                    }

                                    if (searchResult != null) {
                                        val localAnime = networkToLocalAnime.await(searchResult)
                                        val episodes = try {
                                            if (source is EHentai) {
                                                source.getEpisodeList(localAnime.toSAnime(), throttleManager::throttle)
                                            } else {
                                                source.getEpisodeList(localAnime.toSAnime())
                                            }
                                        } catch (e: Exception) {
                                            this@MigrationListScreenModel.logcat(LogPriority.ERROR, e)
                                            emptyList()
                                        }
                                        syncEpisodesWithSource.await(episodes, localAnime, source)
                                        localAnime
                                    } else {
                                        null
                                    }
                                } catch (e: CancellationException) {
                                    // Ignore cancellations
                                    throw e
                                } catch (e: Exception) {
                                    null
                                }
                                anime.progress.value = validSources.size to (index + 1)
                                if (searchResult != null) return@async searchResult
                            }

                            null
                        }
                    }
                    // KMK -->
                    anime.searchingJob?.await()
                    // KMK <--
                } catch (e: CancellationException) {
                    // Ignore canceled migrations
                    continue
                }

                if (result != null && result.thumbnailUrl == null) {
                    try {
                        val newAnime = sourceManager.getOrStub(result.source).getAnimeDetails(result.toSAnime())
                        updateAnime.awaitUpdateFromSource(result, newAnime, true)
                    } catch (e: CancellationException) {
                        // Ignore cancellations
                        throw e
                    } catch (e: Exception) {
                    }
                }

                anime.searchResult.value = if (result == null) {
                    SearchResult.NotFound
                } else {
                    SearchResult.Result(result.id)
                }
                if (result == null && hideNotFound) {
                    removeAnime(anime)
                }
                if (result != null &&
                    showOnlyUpdates &&
                    (getEpisodeInfo(result.id).latestEpisode ?: 0.0) <= (anime.episodeInfo.latestEpisode ?: 0.0)
                ) {
                    removeAnime(anime)
                }

                sourceFinished()
            }
        }
    }

    private suspend fun sourceFinished() {
        finishedCount.value = migratingItems.value.orEmpty().count {
            it.searchResult.value != SearchResult.Searching
        }
        if (allAnimesDone()) {
            migrationDone.value = true
        }
        if (migratingItems.value?.isEmpty() == true) {
            navigateOut()
        }
    }

    fun allAnimesDone() = migratingItems.value.orEmpty().all { it.searchResult.value != SearchResult.Searching } &&
        migratingItems.value.orEmpty().any { it.searchResult.value is SearchResult.Result }

    fun animesSkipped() = migratingItems.value.orEmpty().count { it.searchResult.value == SearchResult.NotFound }

    private suspend fun migrateAnimeInternal(
        prevAnime: Anime,
        anime: Anime,
        replace: Boolean,
    ) {
        if (prevAnime.id == anime.id) return // Nothing to migrate

        val flags = preferences.migrateFlags().get()
        // Update episodes read
        if (MigrationFlags.hasEpisodes(flags)) {
            val prevAnimeEpisodes = getEpisodesByAnimeId.await(prevAnime.id)
            val maxEpisodeRead = prevAnimeEpisodes.filter(Episode::seen)
                .maxOfOrNull(Episode::episodeNumber)
            val dbEpisodes = getEpisodesByAnimeId.await(anime.id)
            val prevHistoryList = getHistoryByAnimeId.await(prevAnime.id)

            val episodeUpdates = mutableListOf<EpisodeUpdate>()
            val historyUpdates = mutableListOf<HistoryUpdate>()

            dbEpisodes.forEach { episode ->
                if (episode.isRecognizedNumber) {
                    val prevEpisode = prevAnimeEpisodes.find {
                        it.isRecognizedNumber &&
                            it.episodeNumber == episode.episodeNumber
                    }
                    if (prevEpisode != null) {
                        episodeUpdates += EpisodeUpdate(
                            id = episode.id,
                            bookmark = prevEpisode.bookmark,
                            seen = prevEpisode.seen,
                            dateFetch = prevEpisode.dateFetch,
                        )
                        prevHistoryList.find { it.episodeId == prevEpisode.id }?.let { prevHistory ->
                            historyUpdates += HistoryUpdate(
                                episode.id,
                                prevHistory.seenAt ?: return@let,
                                prevHistory.watchDuration,
                            )
                        }
                    } else if (maxEpisodeRead != null && episode.episodeNumber <= maxEpisodeRead) {
                        episodeUpdates += EpisodeUpdate(
                            id = episode.id,
                            seen = true,
                        )
                    }
                }
            }

            updateEpisode.awaitAll(episodeUpdates)
            upsertHistory.awaitAll(historyUpdates)
        }
        // Update categories
        if (MigrationFlags.hasCategories(flags)) {
            val categories = getCategories.await(prevAnime.id)
            setAnimeCategories.await(anime.id, categories.map { it.id })
        }
        // Update track
        if (MigrationFlags.hasTracks(flags)) {
            val tracks = getTracks.await(prevAnime.id)
            if (tracks.isNotEmpty()) {
                getTracks.await(anime.id).forEach {
                    deleteTrack.await(anime.id, it.trackerId)
                }
                insertTrack.awaitAll(tracks.map { it.copy(animeId = anime.id) })
            }
        }
        // Update custom cover
        if (MigrationFlags.hasCustomCover(flags) && prevAnime.hasCustomCover(coverCache)) {
            coverCache.setCustomCoverToCache(anime, coverCache.getCustomCoverFile(prevAnime.id).inputStream())
        }

        var animeUpdate = AnimeUpdate(anime.id, favorite = true, dateAdded = System.currentTimeMillis())
        var prevAnimeUpdate: AnimeUpdate? = null
        // Update extras
        if (MigrationFlags.hasExtra(flags)) {
            animeUpdate = animeUpdate.copy(
                episodeFlags = prevAnime.episodeFlags,
                viewerFlags = prevAnime.viewerFlags,
            )
        }
        // Delete downloaded
        if (MigrationFlags.hasDeleteEpisodes(flags)) {
            val oldSource = sourceManager.get(prevAnime.source)
            if (oldSource != null) {
                downloadManager.deleteAnime(prevAnime, oldSource)
            }
        }
        // Update favorite status
        if (replace) {
            prevAnimeUpdate = AnimeUpdate(
                id = prevAnime.id,
                favorite = false,
                dateAdded = 0,
            )
            animeUpdate = animeUpdate.copy(
                dateAdded = prevAnime.dateAdded,
            )
        }

        updateAnime.awaitAll(listOfNotNull(animeUpdate, prevAnimeUpdate))
    }

    /** Set a anime picked from manual search to be used as migration target */
    fun useAnimeForMigration(context: Context, newAnimeId: Long, selectedAnimeId: Long) {
        val migratingAnime = migratingItems.value.orEmpty().find { it.anime.id == selectedAnimeId }
            ?: return
        migratingAnime.searchResult.value = SearchResult.Searching
        screenModelScope.launchIO {
            val result = migratingAnime.migrationScope.async {
                val anime = getAnime(newAnimeId)!!
                val localAnime = networkToLocalAnime.await(anime)
                try {
                    val source = sourceManager.get(anime.source)!!
                    val episodes = source.getEpisodeList(localAnime.toSAnime())
                    syncEpisodesWithSource.await(episodes, localAnime, source)
                } catch (e: Exception) {
                    return@async null
                }
                localAnime
            }.await()

            if (result != null) {
                try {
                    val newAnime = sourceManager.getOrStub(result.source).getAnimeDetails(result.toSAnime())
                    updateAnime.awaitUpdateFromSource(result, newAnime, true)
                } catch (e: CancellationException) {
                    // Ignore cancellations
                    throw e
                } catch (e: Exception) {
                }

                migratingAnime.searchResult.value = SearchResult.Result(result.id)
            } else {
                migratingAnime.searchResult.value = SearchResult.NotFound
                withUIContext {
                    context.toast(SYMR.strings.no_episodes_found_for_migration, Toast.LENGTH_LONG)
                }
            }

            // KMK -->
            sourceFinished()
            // KMK <--
        }
    }

    fun migrateAnimes() {
        migrateAnimes(true)
    }

    fun copyAnimes() {
        migrateAnimes(false)
    }

    private fun migrateAnimes(replace: Boolean) {
        dialog.value = null
        migrateJob = screenModelScope.launchIO {
            migratingProgress.value = 0f
            val items = migratingItems.value.orEmpty()
            try {
                items.forEachIndexed { index, anime ->
                    try {
                        ensureActive()
                        val toAnimeObj = anime.searchResult.value.let {
                            if (it is SearchResult.Result) {
                                getAnime.await(it.id)
                            } else {
                                null
                            }
                        }
                        if (toAnimeObj != null) {
                            migrateAnimeInternal(
                                anime.anime,
                                toAnimeObj,
                                replace,
                            )
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        logcat(LogPriority.WARN, throwable = e)
                    }
                    migratingProgress.value = index.toFloat() / items.size
                }

                navigateOut()
            } finally {
                migratingProgress.value = Float.MAX_VALUE
                migrateJob = null
            }
        }
    }

    fun cancelMigrate() {
        migrateJob?.cancel()
        migrateJob = null
    }

    private suspend fun navigateOut() {
        navigateOut.emit(Unit)
    }

    fun migrateAnime(animeId: Long, copy: Boolean) {
        manualMigrations.value++
        screenModelScope.launchIO {
            val anime = migratingItems.value.orEmpty().find { it.anime.id == animeId }
                ?: return@launchIO

            val toAnimeObj = getAnime.await((anime.searchResult.value as? SearchResult.Result)?.id ?: return@launchIO)
                ?: return@launchIO
            migrateAnimeInternal(
                anime.anime,
                toAnimeObj,
                !copy,
            )

            removeAnime(animeId)
        }
    }

    // KMK -->
    /** Cancel searching without remove it from list so user can perform manual search */
    fun cancelAnime(animeId: Long) {
        screenModelScope.launchIO {
            val item = migratingItems.value.orEmpty().find { it.anime.id == animeId }
                ?: return@launchIO
            item.searchingJob?.cancel()
            item.searchingJob = null
            item.searchResult.value = SearchResult.NotFound
            sourceFinished()
        }
    }
    // KMK <--

    fun removeAnime(animeId: Long) {
        screenModelScope.launchIO {
            val item = migratingItems.value.orEmpty().find { it.anime.id == animeId }
                ?: return@launchIO
            removeAnime(item)
            item.migrationScope.cancel()
            sourceFinished()
        }
    }

    fun removeAnime(item: MigratingAnime) {
        when (val migration = config.migration) {
            is MigrationType.AnimeList -> {
                val ids = migration.animeIds.toMutableList()
                val index = ids.indexOf(item.anime.id)
                if (index > -1) {
                    ids.removeAt(index)
                    config.migration = MigrationType.AnimeList(ids)
                    val index2 = migratingItems.value.orEmpty().indexOf(item)
                    if (index2 > -1) migratingItems.value = (migratingItems.value.orEmpty() - item).toImmutableList()
                }
            }
            is MigrationType.AnimeSingle -> Unit
        }
    }

    override fun onDispose() {
        super.onDispose()
        migratingItems.value.orEmpty().forEach {
            it.migrationScope.cancel()
        }
    }

    fun openMigrateDialog(
        copy: Boolean,
    ) {
        dialog.value = Dialog.MigrateAnimeDialog(
            copy,
            migratingItems.value.orEmpty().size,
            animesSkipped(),
        )
    }

    sealed class Dialog {
        data class MigrateAnimeDialog(val copy: Boolean, val animeSet: Int, val animeSkipped: Int) : Dialog()
        object MigrationExitDialog : Dialog()
    }
}
