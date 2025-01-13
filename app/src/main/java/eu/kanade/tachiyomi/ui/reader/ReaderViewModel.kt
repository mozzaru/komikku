package eu.kanade.tachiyomi.ui.reader

import android.app.Application
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.domain.anime.interactor.SetAnimeViewerFlags
import eu.kanade.domain.anime.model.readerOrientation
import eu.kanade.domain.anime.model.readingMode
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.episode.interactor.SetSeenStatus
import eu.kanade.domain.episode.model.toDbEpisode
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.domain.track.interactor.TrackEpisode
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.data.database.models.toDomainEpisode
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.saver.Image
import eu.kanade.tachiyomi.data.saver.ImageSaver
import eu.kanade.tachiyomi.data.saver.Location
import eu.kanade.tachiyomi.data.sync.SyncDataJob
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.MetadataSource
import eu.kanade.tachiyomi.source.online.all.MergedSource
import eu.kanade.tachiyomi.ui.reader.episode.ReaderEpisodeItem
import eu.kanade.tachiyomi.ui.reader.loader.DownloadPageLoader
import eu.kanade.tachiyomi.ui.reader.loader.EpisodeLoader
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderEpisode
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerEpisodes
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import eu.kanade.tachiyomi.util.editCover
import eu.kanade.tachiyomi.util.episode.filterDownloaded
import eu.kanade.tachiyomi.util.episode.removeDuplicates
import eu.kanade.tachiyomi.util.lang.byteSize
import eu.kanade.tachiyomi.util.lang.takeBytes
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.storage.DiskUtil.MAX_FILE_NAME_BYTES
import eu.kanade.tachiyomi.util.storage.cacheImageDir
import exh.metadata.metadata.RaisedSearchMetadata
import exh.source.MERGED_SOURCE_ID
import exh.source.getMainSource
import exh.source.isEhBasedManga
import exh.util.animeType
import exh.util.defaultReaderType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import tachiyomi.core.common.preference.toggle
import tachiyomi.core.common.storage.UniFileTempFileManager
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.logcat
import tachiyomi.decoder.ImageDecoder
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.anime.interactor.GetFlatMetadataById
import tachiyomi.domain.anime.interactor.GetMergedAnimeById
import tachiyomi.domain.anime.interactor.GetMergedReferencesById
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.episode.interactor.GetMergedEpisodesByAnimeId
import tachiyomi.domain.episode.interactor.UpdateEpisode
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.episode.model.EpisodeUpdate
import tachiyomi.domain.episode.service.getEpisodeSort
import tachiyomi.domain.history.interactor.GetNextEpisodes
import tachiyomi.domain.history.interactor.UpsertHistory
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.util.Date

/**
 * Presenter used by the activity to perform background operations.
 */
class ReaderViewModel @JvmOverloads constructor(
    private val savedState: SavedStateHandle,
    private val sourceManager: SourceManager = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val downloadProvider: DownloadProvider = Injekt.get(),
    private val tempFileManager: UniFileTempFileManager = Injekt.get(),
    private val imageSaver: ImageSaver = Injekt.get(),
    preferences: BasePreferences = Injekt.get(),
    val readerPreferences: ReaderPreferences = Injekt.get(),
    private val basePreferences: BasePreferences = Injekt.get(),
    private val downloadPreferences: DownloadPreferences = Injekt.get(),
    private val trackPreferences: TrackPreferences = Injekt.get(),
    private val trackEpisode: TrackEpisode = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val getEpisodesByAnimeId: GetEpisodesByAnimeId = Injekt.get(),
    private val getNextEpisodes: GetNextEpisodes = Injekt.get(),
    private val upsertHistory: UpsertHistory = Injekt.get(),
    private val updateEpisode: UpdateEpisode = Injekt.get(),
    private val setAnimeViewerFlags: SetAnimeViewerFlags = Injekt.get(),
    private val syncPreferences: SyncPreferences = Injekt.get(),
    // SY -->
    private val uiPreferences: UiPreferences = Injekt.get(),
    private val getFlatMetadataById: GetFlatMetadataById = Injekt.get(),
    private val getMergedAnimeById: GetMergedAnimeById = Injekt.get(),
    private val getMergedReferencesById: GetMergedReferencesById = Injekt.get(),
    private val getMergedEpisodesByAnimeId: GetMergedEpisodesByAnimeId = Injekt.get(),
    private val setSeenStatus: SetSeenStatus = Injekt.get(),
    // SY <--
) : ViewModel() {

    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    /**
     * The anime loaded in the reader. It can be null when instantiated for a short time.
     */
    val anime: Anime?
        get() = state.value.anime

    /**
     * The episode id of the currently loaded episode. Used to restore from process kill.
     */
    private var episodeId = savedState.get<Long>("episode_id") ?: -1L
        set(value) {
            savedState["episode_id"] = value
            field = value
        }

    /**
     * The visible page index of the currently loaded episode. Used to restore from process kill.
     */
    private var episodePageIndex = savedState.get<Int>("page_index") ?: -1
        set(value) {
            savedState["page_index"] = value
            field = value
        }

    /**
     * The episode loader for the loaded anime. It'll be null until [anime] is set.
     */
    private var loader: EpisodeLoader? = null

    /**
     * The time the episode was started reading
     */
    private var episodeReadStartTime: Long? = null

    private var episodeToDownload: Download? = null

    /**
     * Episode list for the active anime. It's retrieved lazily and should be accessed for the first
     * time in a background thread to avoid blocking the UI.
     */
    private val episodeList by lazy {
        val anime = anime!!
        // SY -->
        val (episodes, animeMap) = runBlocking {
            if (anime.source == MERGED_SOURCE_ID) {
                getMergedEpisodesByAnimeId.await(anime.id, applyScanlatorFilter = true) to
                    getMergedAnimeById.await(anime.id)
                        .associateBy { it.id }
            } else {
                getEpisodesByAnimeId.await(anime.id, applyScanlatorFilter = true) to null
            }
        }
        fun isEpisodeDownloaded(episode: Episode): Boolean {
            val episodeAnime = animeMap?.get(episode.animeId) ?: anime
            return downloadManager.isEpisodeDownloaded(
                episodeName = episode.name,
                episodeScanlator = episode.scanlator,
                animeTitle = episodeAnime.ogTitle,
                sourceId = episodeAnime.source,
            )
        }
        // SY <--

        val selectedEpisode = episodes.find { it.id == episodeId }
            ?: error("Requested episode of id $episodeId not found in episode list")

        val episodesForReader = when {
            (readerPreferences.skipRead().get() || readerPreferences.skipFiltered().get()) -> {
                val filteredEpisodes = episodes.filterNot {
                    when {
                        readerPreferences.skipRead().get() && it.seen -> true
                        readerPreferences.skipFiltered().get() -> {
                            (anime.unseenFilterRaw == Anime.EPISODE_SHOW_SEEN && !it.seen) ||
                                (anime.unseenFilterRaw == Anime.EPISODE_SHOW_UNSEEN && it.seen) ||
                                // SY -->
                                (
                                    anime.downloadedFilterRaw == Anime.EPISODE_SHOW_DOWNLOADED &&
                                        !isEpisodeDownloaded(it)
                                    ) ||
                                (
                                    anime.downloadedFilterRaw == Anime.EPISODE_SHOW_NOT_DOWNLOADED &&
                                        isEpisodeDownloaded(it)
                                    ) ||
                                // SY <--
                                (anime.bookmarkedFilterRaw == Anime.EPISODE_SHOW_BOOKMARKED && !it.bookmark) ||
                                (anime.bookmarkedFilterRaw == Anime.EPISODE_SHOW_NOT_BOOKMARKED && it.bookmark)
                        }
                        else -> false
                    }
                }

                if (filteredEpisodes.any { it.id == episodeId }) {
                    filteredEpisodes
                } else {
                    filteredEpisodes + listOf(selectedEpisode)
                }
            }
            else -> episodes
        }

        episodesForReader
            .sortedWith(getEpisodeSort(anime, sortDescending = false))
            .run {
                if (readerPreferences.skipDupe().get()) {
                    removeDuplicates(selectedEpisode)
                } else {
                    this
                }
            }
            .run {
                if (basePreferences.downloadedOnly().get()) {
                    filterDownloaded(anime, animeMap)
                } else {
                    this
                }
            }
            .map { it.toDbEpisode() }
            .map(::ReaderEpisode)
    }

    private val incognitoMode = preferences.incognitoMode().get()
    private val downloadAheadAmount = downloadPreferences.autoDownloadWhileWatching().get()

    init {
        // To save state
        state.map { it.viewerEpisodes?.currEpisode }
            .distinctUntilChanged()
            .filterNotNull()
            // SY -->
            .drop(1) // allow the loader to set the first page and episode id
            // SY <-
            .onEach { currentEpisode ->
                if (episodePageIndex >= 0) {
                    // Restore from SavedState
                    currentEpisode.requestedPage = episodePageIndex
                } else if (!currentEpisode.episode.read) {
                    currentEpisode.requestedPage = currentEpisode.episode.last_page_read
                }
                episodeId = currentEpisode.episode.id!!
            }
            .launchIn(viewModelScope)

        // SY -->
        state.mapLatest { it.ehAutoscrollFreq }
            .distinctUntilChanged()
            .drop(1)
            .onEach { text ->
                val parsed = text.toDoubleOrNull()

                if (parsed == null || parsed <= 0 || parsed > 9999) {
                    readerPreferences.autoscrollInterval().set(-1f)
                    mutableState.update { it.copy(isAutoScrollEnabled = false) }
                } else {
                    readerPreferences.autoscrollInterval().set(parsed.toFloat())
                    mutableState.update { it.copy(isAutoScrollEnabled = true) }
                }
            }
            .launchIn(viewModelScope)
        // SY <--
    }

    override fun onCleared() {
        val currentEpisodes = state.value.viewerEpisodes
        if (currentEpisodes != null) {
            currentEpisodes.unref()
            episodeToDownload?.let {
                downloadManager.addDownloadsToStartOfQueue(listOf(it))
            }
        }
    }

    /**
     * Called when the user pressed the back button and is going to leave the reader. Used to
     * trigger deletion of the downloaded episodes.
     */
    fun onActivityFinish() {
        deletePendingEpisodes()
    }

    /**
     * Whether this presenter is initialized yet.
     */
    fun needsInit(): Boolean {
        return anime == null
    }

    /**
     * Initializes this presenter with the given [animeId] and [initialEpisodeId]. This method will
     * fetch the anime from the database and initialize the initial episode.
     */
    suspend fun init(animeId: Long, initialEpisodeId: Long /* SY --> */, page: Int?/* SY <-- */): Result<Boolean> {
        if (!needsInit()) return Result.success(true)
        return withIOContext {
            try {
                val anime = getAnime.await(animeId)
                if (anime != null) {
                    // SY -->
                    sourceManager.isInitialized.first { it }
                    val source = sourceManager.getOrStub(anime.source)
                    val metadataSource = source.getMainSource<MetadataSource<*, *>>()
                    val metadata = if (metadataSource != null) {
                        getFlatMetadataById.await(animeId)?.raise(metadataSource.metaClass)
                    } else {
                        null
                    }
                    val mergedReferences = if (source is MergedSource) {
                        runBlocking {
                            getMergedReferencesById.await(anime.id)
                        }
                    } else {
                        emptyList()
                    }
                    val mergedAnime = if (source is MergedSource) {
                        runBlocking {
                            getMergedAnimeById.await(anime.id)
                        }.associateBy { it.id }
                    } else {
                        emptyMap()
                    }
                    val relativeTime = uiPreferences.relativeTime().get()
                    val autoScrollFreq = readerPreferences.autoscrollInterval().get()
                    // SY <--
                    mutableState.update {
                        it.copy(
                            anime = anime,
                            /* SY --> */
                            meta = metadata,
                            mergedAnime = mergedAnime,
                            dateRelativeTime = relativeTime,
                            ehAutoscrollFreq = if (autoScrollFreq == -1f) {
                                ""
                            } else {
                                autoScrollFreq.toString()
                            },
                            isAutoScrollEnabled = autoScrollFreq != -1f,
                            /* SY <-- */
                        )
                    }
                    if (episodeId == -1L) episodeId = initialEpisodeId

                    val context = Injekt.get<Application>()
                    // val source = sourceManager.getOrStub(anime.source)
                    loader = EpisodeLoader(
                        context = context,
                        downloadManager = downloadManager,
                        downloadProvider = downloadProvider,
                        anime = anime,
                        source = source, /* SY --> */
                        sourceManager = sourceManager,
                        readerPrefs = readerPreferences,
                        mergedReferences = mergedReferences,
                        mergedAnime = mergedAnime, /* SY <-- */
                    )

                    loadEpisode(
                        loader!!,
                        episodeList.first { episodeId == it.episode.id },
                        /* SY --> */page, /* SY <-- */
                    )
                    Result.success(true)
                } else {
                    // Unlikely but okay
                    Result.success(false)
                }
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    throw e
                }
                Result.failure(e)
            }
        }
    }

    // SY -->
    fun getEpisodes(): List<ReaderEpisodeItem> {
        val currentEpisode = getCurrentEpisode()

        return episodeList.map {
            ReaderEpisodeItem(
                episode = it.episode.toDomainEpisode()!!,
                anime = anime!!,
                isCurrent = it.episode.id == currentEpisode?.episode?.id,
                dateFormat = UiPreferences.dateFormat(uiPreferences.dateFormat().get()),
            )
        }
    }
    // SY <--

    /**
     * Loads the given [episode] with this [loader] and updates the currently active episodes.
     * Callers must handle errors.
     */
    private suspend fun loadEpisode(
        loader: EpisodeLoader,
        episode: ReaderEpisode,
        // SY -->
        page: Int? = null,
        // SY <--
    ): ViewerEpisodes {
        loader.loadEpisode(episode /* SY --> */, page/* SY <-- */)

        val episodePos = episodeList.indexOf(episode)
        val newEpisodes = ViewerEpisodes(
            episode,
            episodeList.getOrNull(episodePos - 1),
            episodeList.getOrNull(episodePos + 1),
        )

        withUIContext {
            mutableState.update {
                // Add new references first to avoid unnecessary recycling
                newEpisodes.ref()
                it.viewerEpisodes?.unref()

                episodeToDownload = cancelQueuedDownloads(newEpisodes.currEpisode)
                it.copy(
                    viewerEpisodes = newEpisodes,
                    bookmarked = newEpisodes.currEpisode.episode.bookmark,
                )
            }
        }
        return newEpisodes
    }

    /**
     * Called when the user changed to the given [episode] when changing pages from the viewer.
     * It's used only to set this episode as active.
     */
    private fun loadNewEpisode(episode: ReaderEpisode) {
        val loader = loader ?: return

        viewModelScope.launchIO {
            logcat { "Loading ${episode.episode.url}" }

            flushReadTimer()
            restartReadTimer()

            try {
                loadEpisode(loader, episode)
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    throw e
                }
                logcat(LogPriority.ERROR, e)
            }
        }
    }

    fun loadNewEpisodeFromDialog(episode: Episode) {
        viewModelScope.launchIO {
            val newEpisode = episodeList.firstOrNull { it.episode.id == episode.id } ?: return@launchIO
            loadAdjacent(newEpisode)
        }
    }

    /**
     * Called when the user is going to load the prev/next episode through the toolbar buttons.
     */
    private suspend fun loadAdjacent(episode: ReaderEpisode) {
        val loader = loader ?: return

        logcat { "Loading adjacent ${episode.episode.url}" }

        mutableState.update { it.copy(isLoadingAdjacentEpisode = true) }
        try {
            withIOContext {
                loadEpisode(loader, episode)
            }
        } catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }
            logcat(LogPriority.ERROR, e)
        } finally {
            mutableState.update { it.copy(isLoadingAdjacentEpisode = false) }
        }
    }

    /**
     * Called when the viewers decide it's a good time to preload a [episode] and improve the UX so
     * that the user doesn't have to wait too long to continue reading.
     */
    suspend fun preload(episode: ReaderEpisode) {
        if (episode.state is ReaderEpisode.State.Loaded || episode.state == ReaderEpisode.State.Loading) {
            return
        }

        if (episode.pageLoader?.isLocal == false) {
            val anime = anime ?: return
            val dbEpisode = episode.episode
            val isDownloaded = downloadManager.isEpisodeDownloaded(
                dbEpisode.name,
                dbEpisode.scanlator,
                /* SY --> */ anime.ogTitle /* SY <-- */,
                anime.source,
                skipCache = true,
            )
            if (isDownloaded) {
                episode.state = ReaderEpisode.State.Wait
            }
        }

        if (episode.state != ReaderEpisode.State.Wait && episode.state !is ReaderEpisode.State.Error) {
            return
        }

        val loader = loader ?: return
        try {
            logcat { "Preloading ${episode.episode.url}" }
            loader.loadEpisode(episode)
        } catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }
            return
        }
        eventChannel.trySend(Event.ReloadViewerEpisodes)
    }

    fun onViewerLoaded(viewer: Viewer?) {
        mutableState.update {
            it.copy(viewer = viewer)
        }
    }

    /**
     * Called every time a page changes on the reader. Used to mark the flag of episodes being
     * read, update tracking services, enqueue downloaded episode deletion, and updating the active episode if this
     * [page]'s episode is different from the currently active.
     */
    fun onPageSelected(page: ReaderPage, currentPageText: String /* SY --> */, hasExtraPage: Boolean /* SY <-- */) {
        // InsertPage doesn't change page progress
        if (page is InsertPage) {
            return
        }

        // SY -->
        mutableState.update { it.copy(currentPageText = currentPageText) }
        // SY <--

        val selectedEpisode = page.episode
        val pages = selectedEpisode.pages ?: return

        // Save last page read and mark as read if needed
        viewModelScope.launchNonCancellable {
            updateEpisodeProgress(selectedEpisode, page/* SY --> */, hasExtraPage/* SY <-- */)
        }

        if (selectedEpisode != getCurrentEpisode()) {
            logcat { "Setting ${selectedEpisode.episode.url} as active" }
            loadNewEpisode(selectedEpisode)
        }

        val inDownloadRange = page.number.toDouble() / pages.size > 0.25
        if (inDownloadRange) {
            downloadNextEpisodes()
        }

        eventChannel.trySend(Event.PageChanged)
    }

    private fun downloadNextEpisodes() {
        if (downloadAheadAmount == 0) return
        val anime = anime ?: return

        // Only download ahead if current + next episode is already downloaded too to avoid jank
        if (getCurrentEpisode()?.pageLoader !is DownloadPageLoader) return
        val nextEpisode = state.value.viewerEpisodes?.nextEpisode?.episode ?: return

        viewModelScope.launchIO {
            val isNextEpisodeDownloaded = downloadManager.isEpisodeDownloaded(
                nextEpisode.name,
                nextEpisode.scanlator,
                // SY -->
                anime.ogTitle,
                // SY <--
                anime.source,
            )
            if (!isNextEpisodeDownloaded) return@launchIO

            val episodesToDownload = getNextEpisodes.await(anime.id, nextEpisode.id!!).run {
                if (readerPreferences.skipDupe().get()) {
                    removeDuplicates(nextEpisode.toDomainEpisode()!!)
                } else {
                    this
                }
            }.take(downloadAheadAmount)

            downloadManager.downloadEpisodes(
                anime,
                episodesToDownload,
            )
        }
    }

    /**
     * Removes [currentEpisode] from download queue
     * if setting is enabled and [currentEpisode] is queued for download
     */
    private fun cancelQueuedDownloads(currentEpisode: ReaderEpisode): Download? {
        return downloadManager.getQueuedDownloadOrNull(currentEpisode.episode.id!!.toLong())?.also {
            downloadManager.cancelQueuedDownloads(listOf(it))
        }
    }

    /**
     * Determines if deleting option is enabled and nth to last episode actually exists.
     * If both conditions are satisfied enqueues episode for delete
     * @param currentEpisode current episode, which is going to be marked as read.
     */
    private fun deleteEpisodeIfNeeded(currentEpisode: ReaderEpisode) {
        val removeAfterReadSlots = downloadPreferences.removeAfterReadSlots().get()
        if (removeAfterReadSlots == -1) return

        // Determine which episode should be deleted and enqueue
        val currentEpisodePosition = episodeList.indexOf(currentEpisode)
        val episodeToDelete = episodeList.getOrNull(currentEpisodePosition - removeAfterReadSlots)

        // If episode is completely read, no need to download it
        episodeToDownload = null

        if (episodeToDelete != null) {
            enqueueDeleteReadEpisodes(episodeToDelete)
        }
    }

    /**
     * Saves the episode progress (last read page and whether it's read)
     * if incognito mode isn't on.
     */
    private suspend fun updateEpisodeProgress(
        readerEpisode: ReaderEpisode,
        page: Page/* SY --> */,
        hasExtraPage: Boolean, /* SY <-- */
    ) {
        val pageIndex = page.index
        val syncTriggerOpt = syncPreferences.getSyncTriggerOptions()
        val isSyncEnabled = syncPreferences.isSyncEnabled()

        mutableState.update {
            it.copy(currentPage = pageIndex + 1)
        }
        readerEpisode.requestedPage = pageIndex
        episodePageIndex = pageIndex

        if (!incognitoMode && page.status != Page.State.ERROR) {
            readerEpisode.episode.last_page_read = pageIndex

            // SY -->
            if (
                readerEpisode.pages?.lastIndex == pageIndex ||
                (hasExtraPage && readerEpisode.pages?.lastIndex?.minus(1) == page.index)
            ) {
                // SY <--
                readerEpisode.episode.read = true
                // SY -->
                if (readerEpisode.episode.episode_number >= 0 && readerPreferences.markReadDupe().get()) {
                    getEpisodesByAnimeId.await(anime!!.id).sortedByDescending { it.sourceOrder }
                        .filter {
                            it.id != readerEpisode.episode.id &&
                                !it.seen &&
                                it.episodeNumber.toFloat() == readerEpisode.episode.episode_number
                        }
                        .ifEmpty { null }
                        ?.also {
                            setSeenStatus.await(true, *it.toTypedArray())
                            it.forEach { episode ->
                                deleteEpisodeIfNeeded(ReaderEpisode(episode))
                            }
                        }
                }
                if (anime?.isEhBasedManga() == true) {
                    viewModelScope.launchNonCancellable {
                        val episodeUpdates = episodeList
                            .filter { it.episode.source_order > readerEpisode.episode.source_order }
                            .map { episode ->
                                EpisodeUpdate(
                                    id = episode.episode.id!!,
                                    seen = true,
                                )
                            }
                        updateEpisode.awaitAll(episodeUpdates)
                    }
                }
                // SY <--
                updateTrackEpisodeRead(readerEpisode)
                deleteEpisodeIfNeeded(readerEpisode)

                // Check if syncing is enabled for episode read:
                if (isSyncEnabled && syncTriggerOpt.syncOnEpisodeRead) {
                    SyncDataJob.startNow(Injekt.get<Application>())
                }
            }

            updateEpisode.await(
                EpisodeUpdate(
                    id = readerEpisode.episode.id!!,
                    seen = readerEpisode.episode.read,
                    lastSecondSeen = readerEpisode.episode.last_page_read.toLong(),
                ),
            )

            // Check if syncing is enabled for episode open:
            if (isSyncEnabled && syncTriggerOpt.syncOnEpisodeOpen && readerEpisode.episode.last_page_read == 0) {
                SyncDataJob.startNow(Injekt.get<Application>())
            }
        }
    }

    fun restartReadTimer() {
        episodeReadStartTime = Instant.now().toEpochMilli()
    }

    fun flushReadTimer() {
        getCurrentEpisode()?.let {
            viewModelScope.launchNonCancellable {
                updateHistory(it)
            }
        }
    }

    /**
     * Saves the episode last read history if incognito mode isn't on.
     */
    private suspend fun updateHistory(readerEpisode: ReaderEpisode) {
        if (incognitoMode) return

        val episodeId = readerEpisode.episode.id!!
        val endTime = Date()
        val sessionReadDuration = episodeReadStartTime?.let { endTime.time - it } ?: 0

        upsertHistory.await(HistoryUpdate(episodeId, endTime, sessionReadDuration))
        episodeReadStartTime = null
    }

    /**
     * Called from the activity to load and set the next episode as active.
     */
    suspend fun loadNextEpisode() {
        val nextEpisode = state.value.viewerEpisodes?.nextEpisode ?: return
        loadAdjacent(nextEpisode)
    }

    /**
     * Called from the activity to load and set the previous episode as active.
     */
    suspend fun loadPreviousEpisode() {
        val prevEpisode = state.value.viewerEpisodes?.prevEpisode ?: return
        loadAdjacent(prevEpisode)
    }

    /**
     * Returns the currently active episode.
     */
    private fun getCurrentEpisode(): ReaderEpisode? {
        return state.value.currentEpisode
    }

    fun getSource() = anime?.source?.let { sourceManager.getOrStub(it) } as? HttpSource

    fun getEpisodeUrl(): String? {
        val sEpisode = getCurrentEpisode()?.episode ?: return null
        val source = getSource() ?: return null

        return try {
            source.getEpisodeUrl(sEpisode)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }

    /**
     * Bookmarks the currently active episode.
     */
    fun toggleEpisodeBookmark() {
        val episode = getCurrentEpisode()?.episode ?: return
        val bookmarked = !episode.bookmark
        episode.bookmark = bookmarked

        viewModelScope.launchNonCancellable {
            updateEpisode.await(
                EpisodeUpdate(
                    id = episode.id!!.toLong(),
                    bookmark = bookmarked,
                ),
            )
        }

        mutableState.update {
            it.copy(
                bookmarked = bookmarked,
            )
        }
    }

    // SY -->
    fun toggleBookmark(episodeId: Long, bookmarked: Boolean) {
        val episode = episodeList.find { it.episode.id == episodeId }?.episode ?: return
        episode.bookmark = bookmarked
        viewModelScope.launchNonCancellable {
            updateEpisode.await(
                EpisodeUpdate(
                    id = episodeId,
                    bookmark = bookmarked,
                ),
            )
        }
    }
    // SY <--

    /**
     * Returns the viewer position used by this anime or the default one.
     */
    fun getAnimeReadingMode(resolveDefault: Boolean = true): Int {
        val default = readerPreferences.defaultReadingMode().get()
        val anime = anime ?: return default
        val readingMode = ReadingMode.fromPreference(anime.readingMode.toInt())
        // SY -->
        return when {
            resolveDefault && readingMode == ReadingMode.DEFAULT && readerPreferences.useAutoWebtoon().get() -> {
                anime.defaultReaderType(anime.animeType(sourceName = sourceManager.get(anime.source)?.name))
                    ?: default
            }
            resolveDefault && readingMode == ReadingMode.DEFAULT -> default
            else -> anime.readingMode.toInt()
        }
        // SY <--
    }

    /**
     * Updates the viewer position for the open anime.
     */
    fun setAnimeReadingMode(readingMode: ReadingMode) {
        val anime = anime ?: return
        runBlocking(Dispatchers.IO) {
            setAnimeViewerFlags.awaitSetReadingMode(anime.id, readingMode.flagValue.toLong())
            val currEpisodes = state.value.viewerEpisodes
            if (currEpisodes != null) {
                // Save current page
                val currEpisode = currEpisodes.currEpisode
                currEpisode.requestedPage = currEpisode.episode.last_page_read

                mutableState.update {
                    it.copy(
                        anime = getAnime.await(anime.id),
                        viewerEpisodes = currEpisodes,
                    )
                }
                eventChannel.send(Event.ReloadViewerEpisodes)
            }
        }
    }

    /**
     * Returns the orientation type used by this anime or the default one.
     */
    fun getAnimeOrientation(resolveDefault: Boolean = true): Int {
        val default = readerPreferences.defaultOrientationType().get()
        val orientation = ReaderOrientation.fromPreference(anime?.readerOrientation?.toInt())
        return when {
            resolveDefault && orientation == ReaderOrientation.DEFAULT -> default
            else -> anime?.readerOrientation?.toInt() ?: default
        }
    }

    /**
     * Updates the orientation type for the open anime.
     */
    fun setAnimeOrientationType(orientation: ReaderOrientation) {
        val anime = anime ?: return
        viewModelScope.launchIO {
            setAnimeViewerFlags.awaitSetOrientation(anime.id, orientation.flagValue.toLong())
            val currEpisodes = state.value.viewerEpisodes
            if (currEpisodes != null) {
                // Save current page
                val currEpisode = currEpisodes.currEpisode
                currEpisode.requestedPage = currEpisode.episode.last_page_read

                mutableState.update {
                    it.copy(
                        anime = getAnime.await(anime.id),
                        viewerEpisodes = currEpisodes,
                    )
                }
                eventChannel.send(Event.SetOrientation(getAnimeOrientation()))
                eventChannel.send(Event.ReloadViewerEpisodes)
            }
        }
    }

    // SY -->
    fun toggleCropBorders(): Boolean {
        val readingMode = getAnimeReadingMode()
        val isPagerType = ReadingMode.isPagerType(readingMode)
        val isWebtoon = ReadingMode.WEBTOON.flagValue == readingMode
        return if (isPagerType) {
            readerPreferences.cropBorders().toggle()
        } else if (isWebtoon) {
            readerPreferences.cropBordersWebtoon().toggle()
        } else {
            readerPreferences.cropBordersContinuousVertical().toggle()
        }
    }
    // SY <--

    /**
     * Generate a filename for the given [anime] and [page]
     */
    private fun generateFilename(
        anime: Anime,
        page: ReaderPage,
    ): String {
        val episode = page.episode.episode
        val filenameSuffix = " - ${page.number}"
        return DiskUtil.buildValidFilename(
            "${anime.title} - ${episode.name}".takeBytes(DiskUtil.MAX_FILE_NAME_BYTES - filenameSuffix.byteSize()),
        ) + filenameSuffix
    }

    fun showMenus(visible: Boolean) {
        mutableState.update { it.copy(menuVisible = visible) }
    }

    // SY -->
    fun showEhUtils(visible: Boolean) {
        mutableState.update { it.copy(ehUtilsVisible = visible) }
    }

    fun setIndexEpisodeToShift(index: Long?) {
        mutableState.update { it.copy(indexEpisodeToShift = index) }
    }

    fun setIndexPageToShift(index: Int?) {
        mutableState.update { it.copy(indexPageToShift = index) }
    }

    fun openEpisodeListDialog() {
        mutableState.update { it.copy(dialog = Dialog.EpisodeList) }
    }

    fun setDoublePages(doublePages: Boolean) {
        mutableState.update { it.copy(doublePages = doublePages) }
    }

    fun openAutoScrollHelpDialog() {
        mutableState.update { it.copy(dialog = Dialog.AutoScrollHelp) }
    }

    fun openBoostPageHelp() {
        mutableState.update { it.copy(dialog = Dialog.BoostPageHelp) }
    }

    fun openRetryAllHelp() {
        mutableState.update { it.copy(dialog = Dialog.RetryAllHelp) }
    }

    fun toggleAutoScroll(enabled: Boolean) {
        mutableState.update { it.copy(autoScroll = enabled) }
    }

    fun setAutoScrollFrequency(frequency: String) {
        mutableState.update { it.copy(ehAutoscrollFreq = frequency) }
    }
    // SY <--

    fun showLoadingDialog() {
        mutableState.update { it.copy(dialog = Dialog.Loading) }
    }

    fun openReadingModeSelectDialog() {
        mutableState.update { it.copy(dialog = Dialog.ReadingModeSelect) }
    }

    fun openOrientationModeSelectDialog() {
        mutableState.update { it.copy(dialog = Dialog.OrientationModeSelect) }
    }

    fun openPageDialog(page: ReaderPage/* SY --> */, extraPage: ReaderPage? = null/* SY <-- */) {
        mutableState.update { it.copy(dialog = Dialog.PageActions(page, extraPage)) }
    }

    fun openSettingsDialog() {
        mutableState.update { it.copy(dialog = Dialog.Settings) }
    }

    fun closeDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    fun setBrightnessOverlayValue(value: Int) {
        mutableState.update { it.copy(brightnessOverlayValue = value) }
    }

    /**
     * Saves the image of the selected page on the pictures directory and notifies the UI of the result.
     * There's also a notification to allow sharing the image somewhere else or deleting it.
     */
    fun saveImage(useExtraPage: Boolean) {
        // SY -->
        val page = if (useExtraPage) {
            (state.value.dialog as? Dialog.PageActions)?.extraPage
        } else {
            (state.value.dialog as? Dialog.PageActions)?.page
        }
        // SY <--
        if (page?.status != Page.State.READY) return
        val anime = anime ?: return

        val context = Injekt.get<Application>()
        val notifier = SaveImageNotifier(context)
        notifier.onClear()

        val filename = generateFilename(anime, page)

        // Pictures directory.
        val relativePath = if (readerPreferences.folderPerAnime().get()) {
            DiskUtil.buildValidFilename(anime.title)
        } else {
            ""
        }

        // Copy file in background.
        viewModelScope.launchNonCancellable {
            try {
                val uri = imageSaver.save(
                    image = Image.Page(
                        inputStream = page.stream!!,
                        name = filename,
                        location = Location.Pictures.create(relativePath),
                    ),
                )
                withUIContext {
                    notifier.onComplete(uri)
                    eventChannel.send(Event.SavedImage(SaveImageResult.Success(uri)))
                }
            } catch (e: Throwable) {
                notifier.onError(e.message)
                eventChannel.send(Event.SavedImage(SaveImageResult.Error(e)))
            }
        }
    }

    // SY -->
    fun saveImages() {
        val (firstPage, secondPage) = (state.value.dialog as? Dialog.PageActions ?: return)
        val viewer = state.value.viewer as? PagerViewer ?: return
        val isLTR = (viewer !is R2LPagerViewer) xor (viewer.config.invertDoublePages)
        val bg = viewer.config.pageCanvasColor

        if (firstPage.status != Page.State.READY) return
        if (secondPage?.status != Page.State.READY) return

        val anime = anime ?: return

        val context = Injekt.get<Application>()
        val notifier = SaveImageNotifier(context)
        notifier.onClear()

        // Copy file in background.
        viewModelScope.launchNonCancellable {
            try {
                val uri = saveImages(
                    page1 = firstPage,
                    page2 = secondPage,
                    isLTR = isLTR,
                    bg = bg,
                    location = Location.Pictures.create(DiskUtil.buildValidFilename(anime.title)),
                    anime = anime,
                )
                eventChannel.send(Event.SavedImage(SaveImageResult.Success(uri)))
            } catch (e: Throwable) {
                notifier.onError(e.message)
                eventChannel.send(Event.SavedImage(SaveImageResult.Error(e)))
            }
        }
    }

    private fun saveImages(
        page1: ReaderPage,
        page2: ReaderPage,
        isLTR: Boolean,
        @ColorInt bg: Int,
        location: Location,
        anime: Anime,
    ): Uri {
        val stream1 = page1.stream!!
        ImageUtil.findImageType(stream1) ?: throw Exception("Not an image")
        val stream2 = page2.stream!!
        ImageUtil.findImageType(stream2) ?: throw Exception("Not an image")
        val imageBitmap = ImageDecoder.newInstance(stream1())?.decode()!!
        val imageBitmap2 = ImageDecoder.newInstance(stream2())?.decode()!!

        val episode = page1.episode.episode

        // Build destination file.
        val filenameSuffix = " - ${page1.number}-${page2.number}.jpg"
        val filename = DiskUtil.buildValidFilename(
            "${anime.title} - ${episode.name}".takeBytes(MAX_FILE_NAME_BYTES - filenameSuffix.byteSize()),
        ) + filenameSuffix

        return imageSaver.save(
            image = Image.Page(
                inputStream = { ImageUtil.mergeBitmaps(imageBitmap, imageBitmap2, isLTR, 0, bg).inputStream() },
                name = filename,
                location = location,
            ),
        )
    }
    // SY <--

    /**
     * Shares the image of the selected page and notifies the UI with the path of the file to share.
     * The image must be first copied to the internal partition because there are many possible
     * formats it can come from, like a zipped episode, in which case it's not possible to directly
     * get a path to the file and it has to be decompressed somewhere first. Only the last shared
     * image will be kept so it won't be taking lots of internal disk space.
     */
    fun shareImage(
        copyToClipboard: Boolean,
        // SY -->
        useExtraPage: Boolean,
        // SY <--
    ) {
        // SY -->
        val page = if (useExtraPage) {
            (state.value.dialog as? Dialog.PageActions)?.extraPage
        } else {
            (state.value.dialog as? Dialog.PageActions)?.page
        }
        // SY <--
        if (page?.status != Page.State.READY) return
        val anime = anime ?: return

        val context = Injekt.get<Application>()
        val destDir = context.cacheImageDir

        val filename = generateFilename(anime, page)

        try {
            viewModelScope.launchNonCancellable {
                destDir.deleteRecursively()
                val uri = imageSaver.save(
                    image = Image.Page(
                        inputStream = page.stream!!,
                        name = filename,
                        location = Location.Cache,
                    ),
                )
                eventChannel.send(if (copyToClipboard) Event.CopyImage(uri) else Event.ShareImage(uri, page))
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e)
        }
    }

    // SY -->
    fun shareImages(copyToClipboard: Boolean) {
        val (firstPage, secondPage) = (state.value.dialog as? Dialog.PageActions ?: return)
        val viewer = state.value.viewer as? PagerViewer ?: return
        val isLTR = (viewer !is R2LPagerViewer) xor (viewer.config.invertDoublePages)
        val bg = viewer.config.pageCanvasColor

        if (firstPage.status != Page.State.READY) return
        if (secondPage?.status != Page.State.READY) return
        val anime = anime ?: return

        val context = Injekt.get<Application>()
        val destDir = context.cacheImageDir

        try {
            viewModelScope.launchNonCancellable {
                destDir.deleteRecursively()
                val uri = saveImages(
                    page1 = firstPage,
                    page2 = secondPage,
                    isLTR = isLTR,
                    bg = bg,
                    location = Location.Cache,
                    anime = anime,
                )
                eventChannel.send(if (copyToClipboard) Event.CopyImage(uri) else Event.ShareImage(uri, firstPage, secondPage))
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e)
        }
    }
    // SY <--

    /**
     * Sets the image of the selected page as cover and notifies the UI of the result.
     */
    fun setAsCover(useExtraPage: Boolean) {
        // SY -->
        val page = if (useExtraPage) {
            (state.value.dialog as? Dialog.PageActions)?.extraPage
        } else {
            (state.value.dialog as? Dialog.PageActions)?.page
        }
        // SY <--
        if (page?.status != Page.State.READY) return
        val anime = anime ?: return
        val stream = page.stream ?: return

        viewModelScope.launchNonCancellable {
            val result = try {
                anime.editCover(Injekt.get(), stream())
                if (anime.isLocal() || anime.favorite) {
                    SetAsCoverResult.Success
                } else {
                    SetAsCoverResult.AddToLibraryFirst
                }
            } catch (e: Exception) {
                SetAsCoverResult.Error
            }
            eventChannel.send(Event.SetCoverResult(result))
        }
    }

    enum class SetAsCoverResult {
        Success,
        AddToLibraryFirst,
        Error,
    }

    sealed interface SaveImageResult {
        class Success(val uri: Uri) : SaveImageResult
        class Error(val error: Throwable) : SaveImageResult
    }

    /**
     * Starts the service that updates the last episode read in sync services. This operation
     * will run in a background thread and errors are ignored.
     */
    private fun updateTrackEpisodeRead(readerEpisode: ReaderEpisode) {
        if (incognitoMode) return
        if (!trackPreferences.autoUpdateTrack().get()) return

        val anime = anime ?: return
        val context = Injekt.get<Application>()

        viewModelScope.launchNonCancellable {
            trackEpisode.await(context, anime.id, readerEpisode.episode.episode_number.toDouble())
        }
    }

    /**
     * Enqueues this [episode] to be deleted when [deletePendingEpisodes] is called. The download
     * manager handles persisting it across process deaths.
     */
    private fun enqueueDeleteReadEpisodes(episode: ReaderEpisode) {
        if (!episode.episode.read) return
        val mergedAnime = state.value.mergedAnime
        // SY -->
        val anime = if (mergedAnime.isNullOrEmpty()) {
            anime
        } else {
            mergedAnime[episode.episode.anime_id]
        } ?: return
        // SY <--

        viewModelScope.launchNonCancellable {
            downloadManager.enqueueEpisodesToDelete(listOf(episode.episode.toDomainEpisode()!!), anime)
        }
    }

    /**
     * Deletes all the pending episodes. This operation will run in a background thread and errors
     * are ignored.
     */
    private fun deletePendingEpisodes() {
        viewModelScope.launchNonCancellable {
            downloadManager.deletePendingEpisodes()
            tempFileManager.deleteTempFiles()
        }
    }

    @Immutable
    data class State(
        val anime: Anime? = null,
        val viewerEpisodes: ViewerEpisodes? = null,
        val bookmarked: Boolean = false,
        val isLoadingAdjacentEpisode: Boolean = false,
        val currentPage: Int = -1,

        /**
         * Viewer used to display the pages (pager, webtoon, ...).
         */
        val viewer: Viewer? = null,
        val dialog: Dialog? = null,
        val menuVisible: Boolean = false,
        @IntRange(from = -100, to = 100) val brightnessOverlayValue: Int = 0,

        // SY -->
        /** for display page number in double-page mode */
        val currentPageText: String = "",
        val meta: RaisedSearchMetadata? = null,
        val mergedAnime: Map<Long, Anime>? = null,
        val ehUtilsVisible: Boolean = false,
        val lastShiftDoubleState: Boolean? = null,
        val indexPageToShift: Int? = null,
        val indexEpisodeToShift: Long? = null,
        val doublePages: Boolean = false,
        val dateRelativeTime: Boolean = true,
        val autoScroll: Boolean = false,
        val isAutoScrollEnabled: Boolean = false,
        val ehAutoscrollFreq: String = "",
        // SY <--
    ) {
        val currentEpisode: ReaderEpisode?
            get() = viewerEpisodes?.currEpisode

        val totalPages: Int
            get() = currentEpisode?.pages?.size ?: -1
    }

    sealed interface Dialog {
        data object Loading : Dialog
        data object Settings : Dialog
        data object ReadingModeSelect : Dialog
        data object OrientationModeSelect : Dialog

        // SY -->
        data object EpisodeList : Dialog
        // SY <--

        data class PageActions(
            val page: ReaderPage/* SY --> */,
            val extraPage: ReaderPage? = null, /* SY <-- */
        ) : Dialog

        // SY -->
        data object AutoScrollHelp : Dialog
        data object RetryAllHelp : Dialog
        data object BoostPageHelp : Dialog
        // SY <--
    }

    sealed interface Event {
        data object ReloadViewerEpisodes : Event
        data object PageChanged : Event
        data class SetOrientation(val orientation: Int) : Event
        data class SetCoverResult(val result: SetAsCoverResult) : Event

        data class SavedImage(val result: SaveImageResult) : Event
        data class ShareImage(
            val uri: Uri,
            val page: ReaderPage/* SY --> */,
            val secondPage: ReaderPage? = null, /* SY <-- */
        ) : Event
        data class CopyImage(val uri: Uri) : Event
    }
}
