package eu.kanade.tachiyomi.ui.anime

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastAny
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.palette.graphics.Palette
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import coil3.Image
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import eu.kanade.core.preference.asState
import eu.kanade.core.util.addOrRemove
import eu.kanade.core.util.insertSeparators
import eu.kanade.domain.anime.interactor.GetExcludedScanlators
import eu.kanade.domain.anime.interactor.SetExcludedScanlators
import eu.kanade.domain.anime.interactor.SmartSearchMerge
import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.domain.anime.model.chaptersFiltered
import eu.kanade.domain.anime.model.downloadedFilter
import eu.kanade.domain.anime.model.toDomainAnime
import eu.kanade.domain.anime.model.toSAnime
import eu.kanade.domain.episode.interactor.GetAvailableScanlators
import eu.kanade.domain.episode.interactor.SetSeenStatus
import eu.kanade.domain.episode.interactor.SyncEpisodesWithSource
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.domain.track.interactor.RefreshTracks
import eu.kanade.domain.track.interactor.TrackEpisode
import eu.kanade.domain.track.model.AutoTrackState
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.presentation.manga.components.EpisodeDownloadAction
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.data.coil.getBestColor
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.getNameForAnimeInfo
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.all.MergedSource
import eu.kanade.tachiyomi.ui.anime.RelatedAnime.Companion.isLoading
import eu.kanade.tachiyomi.ui.anime.RelatedAnime.Companion.removeDuplicates
import eu.kanade.tachiyomi.ui.anime.RelatedAnime.Companion.sorted
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.util.episode.getNextUnseen
import eu.kanade.tachiyomi.util.removeCovers
import eu.kanade.tachiyomi.util.system.getBitmapOrNull
import eu.kanade.tachiyomi.util.system.toast
import exh.source.MERGED_SOURCE_ID
import exh.util.nullIfEmpty
import exh.util.trimOrNull
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.domain.chapter.interactor.FilterChaptersForDownload
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.mapAsCheckboxState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.source.NoResultsException
import tachiyomi.domain.manga.interactor.DeleteMergeById
import tachiyomi.domain.manga.interactor.GetAnime
import tachiyomi.domain.manga.interactor.GetAnimeWithEpisodes
import tachiyomi.domain.manga.interactor.GetDuplicateLibraryAnime
import tachiyomi.domain.manga.interactor.GetMergedAnimeById
import tachiyomi.domain.manga.interactor.GetMergedReferencesById
import tachiyomi.domain.manga.interactor.NetworkToLocalAnime
import tachiyomi.domain.manga.interactor.SetAnimeEpisodeFlags
import tachiyomi.domain.manga.interactor.SetCustomAnimeInfo
import tachiyomi.domain.manga.interactor.UpdateMergedSettings
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.model.MergeMangaSettingsUpdate
import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.domain.manga.model.applyFilter
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.interactor.GetMergedChaptersByMangaId
import tachiyomi.domain.chapter.interactor.SetAnimeDefaultEpisodeFlags
import tachiyomi.domain.chapter.interactor.UpdateEpisode
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.EpisodeUpdate
import tachiyomi.domain.chapter.service.calculateEpisodeGap
import tachiyomi.domain.chapter.service.getEpisodeSort
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.i18n.MR
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.floor
import androidx.compose.runtime.State as RuntimeState

class AnimeScreenModel(
    private val context: Context,
    private val lifecycle: Lifecycle,
    private val mangaId: Long,
    // SY -->
    /** If it is opened from Source then it will auto expand the manga description */
    private val isFromSource: Boolean,
    private val smartSearched: Boolean,
    // SY <--
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val trackPreferences: TrackPreferences = Injekt.get(),
    readerPreferences: ReaderPreferences = Injekt.get(),
    private val uiPreferences: UiPreferences = Injekt.get(),
    // KMK -->
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    // KMK <--
    private val trackerManager: TrackerManager = Injekt.get(),
    private val trackEpisode: TrackEpisode = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val downloadCache: DownloadCache = Injekt.get(),
    private val getAnimeWithEpisodes: GetAnimeWithEpisodes = Injekt.get(),
    // SY -->
    private val sourceManager: SourceManager = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val getMergedChaptersByMangaId: GetMergedChaptersByMangaId = Injekt.get(),
    private val getMergedAnimeById: GetMergedAnimeById = Injekt.get(),
    private val getMergedReferencesById: GetMergedReferencesById = Injekt.get(),
    // KMK -->
    private val smartSearchMerge: SmartSearchMerge = Injekt.get(),
    // KMK <--
    private val updateMergedSettings: UpdateMergedSettings = Injekt.get(),
    val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
    private val deleteMergeById: DeleteMergeById = Injekt.get(),
    private val setCustomAnimeInfo: SetCustomAnimeInfo = Injekt.get(),
    // SY <--
    private val getDuplicateLibraryAnime: GetDuplicateLibraryAnime = Injekt.get(),
    private val getAvailableScanlators: GetAvailableScanlators = Injekt.get(),
    private val getExcludedScanlators: GetExcludedScanlators = Injekt.get(),
    private val setExcludedScanlators: SetExcludedScanlators = Injekt.get(),
    private val setAnimeEpisodeFlags: SetAnimeEpisodeFlags = Injekt.get(),
    private val setAnimeDefaultEpisodeFlags: SetAnimeDefaultEpisodeFlags = Injekt.get(),
    private val setSeenStatus: SetSeenStatus = Injekt.get(),
    private val updateEpisode: UpdateEpisode = Injekt.get(),
    private val updateAnime: UpdateAnime = Injekt.get(),
    private val syncEpisodesWithSource: SyncEpisodesWithSource = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val getTracks: GetTracks = Injekt.get(),
    private val addTracks: AddTracks = Injekt.get(),
    private val setMangaCategories: SetMangaCategories = Injekt.get(),
    private val mangaRepository: MangaRepository = Injekt.get(),
    private val filterChaptersForDownload: FilterChaptersForDownload = Injekt.get(),
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
) : StateScreenModel<AnimeScreenModel.State>(State.Loading) {

    private val successState: State.Success?
        get() = state.value as? State.Success

    // KMK -->
    val useNewSourceNavigation by uiPreferences.useNewSourceNavigation().asState(screenModelScope)
    val themeCoverBased = uiPreferences.themeCoverBased().get()
    // KMK <--

    val manga: Manga?
        get() = successState?.manga

    val source: Source?
        get() = successState?.source

    private val isFavorited: Boolean
        get() = manga?.favorite ?: false

    private val allChapters: List<EpisodeList.Item>?
        get() = successState?.episodes

    private val filteredChapters: List<EpisodeList.Item>?
        get() = successState?.processedEpisodes

    val chapterSwipeStartAction = libraryPreferences.swipeToEndAction().get()
    val chapterSwipeEndAction = libraryPreferences.swipeToStartAction().get()
    private var autoTrackState = trackPreferences.autoUpdateTrackOnMarkRead().get()

    private val skipFiltered by readerPreferences.skipFiltered().asState(screenModelScope)

    val isUpdateIntervalEnabled =
        LibraryPreferences.ANIME_OUTSIDE_RELEASE_PERIOD in libraryPreferences.autoUpdateMangaRestrictions().get()

    private val selectedPositions: Array<Int> = arrayOf(-1, -1) // first and last selected index in list
    private val selectedChapterIds: HashSet<Long> = HashSet()

    // SY -->
    private data class CombineState(
        val manga: Manga,
        val chapters: List<Chapter>,
        val mergedData: MergedAnimeData? = null,
    ) {
        constructor(pair: Pair<Manga, List<Chapter>>) :
            this(pair.first, pair.second)
    }
    // SY <--

    /**
     * Helper function to update the UI state only if it's currently in success state
     */
    private inline fun updateSuccessState(func: (State.Success) -> State.Success) {
        mutableState.update {
            when (it) {
                State.Loading -> it
                is State.Success -> func(it)
            }
        }
    }

    init {
        screenModelScope.launchIO {
            getAnimeWithEpisodes.subscribe(mangaId, applyScanlatorFilter = true).distinctUntilChanged()
                // SY -->
                .combine(
                    getMergedChaptersByMangaId.subscribe(mangaId, true, applyScanlatorFilter = true)
                        .distinctUntilChanged(),
                ) { (manga, chapters), mergedChapters ->
                    if (manga.source == MERGED_SOURCE_ID) {
                        manga to mergedChapters
                    } else {
                        manga to chapters
                    }
                }
                .map { CombineState(it) }
                .combine(
                    combine(
                        getMergedAnimeById.subscribe(mangaId)
                            .distinctUntilChanged(),
                        getMergedReferencesById.subscribe(mangaId)
                            .distinctUntilChanged(),
                    ) { manga, references ->
                        if (manga.isNotEmpty()) {
                            MergedAnimeData(
                                references,
                                manga.associateBy { it.id },
                                references.map { it.mangaSourceId }.distinct()
                                    .map { sourceManager.getOrStub(it) },
                            )
                        } else {
                            null
                        }
                    },
                ) { state, mergedData ->
                    state.copy(mergedData = mergedData)
                }
                .combine(downloadCache.changes) { state, _ -> state }
                .combine(downloadManager.queueState) { state, _ -> state }
                // SY <--
                .flowWithLifecycle(lifecycle)
                .collectLatest { (manga, chapters /* SY --> */, mergedData /* SY <-- */) ->
                    val chapterItems = chapters.toChapterListItems(manga /* SY --> */, mergedData /* SY <-- */)
                    updateSuccessState {
                        it.copy(
                            manga = manga,
                            episodes = chapterItems,
                            // SY -->
                            mergedData = mergedData,
                            // SY <--
                        )
                    }
                }
        }

        screenModelScope.launchIO {
            getExcludedScanlators.subscribe(mangaId)
                .flowWithLifecycle(lifecycle)
                .distinctUntilChanged()
                .collectLatest { excludedScanlators ->
                    updateSuccessState {
                        it.copy(excludedScanlators = excludedScanlators.toImmutableSet())
                    }
                }
        }

        screenModelScope.launchIO {
            getAvailableScanlators.subscribe(mangaId)
                .flowWithLifecycle(lifecycle)
                .distinctUntilChanged()
                // SY -->
                .combine(
                    state.map { (it as? State.Success)?.manga }
                        .distinctUntilChangedBy { it?.source }
                        .flatMapConcat {
                            if (it?.source == MERGED_SOURCE_ID) {
                                getAvailableScanlators.subscribeMerge(mangaId)
                            } else {
                                flowOf(emptySet())
                            }
                        },
                ) { mangaScanlators, mergeScanlators ->
                    mangaScanlators + mergeScanlators
                } // SY <--
                .collectLatest { availableScanlators ->
                    updateSuccessState {
                        it.copy(availableScanlators = availableScanlators.toImmutableSet())
                    }
                }
        }

        observeDownloads()

        screenModelScope.launchIO {
            val manga = getAnimeWithEpisodes.awaitManga(mangaId)

            // SY -->
            val mergedData = getMergedReferencesById.await(mangaId).takeIf { it.isNotEmpty() }?.let { references ->
                MergedAnimeData(
                    references,
                    getMergedAnimeById.await(mangaId).associateBy { it.id },
                    references.map { it.mangaSourceId }.distinct()
                        .map { sourceManager.getOrStub(it) },
                )
            }
            val chapters = (
                if (manga.source ==
                    MERGED_SOURCE_ID
                ) {
                    getMergedChaptersByMangaId.await(mangaId, applyScanlatorFilter = true)
                } else {
                    getAnimeWithEpisodes.awaitChapters(mangaId, applyScanlatorFilter = true)
                }
                )
                .toChapterListItems(manga, mergedData)
            // SY <--

            if (!manga.favorite) {
                setAnimeDefaultEpisodeFlags.await(manga)
            }

            val needRefreshInfo = !manga.initialized
            val needRefreshChapter = chapters.isEmpty()

            // Show what we have earlier
            mutableState.update {
                // SY -->
                val source = sourceManager.getOrStub(manga.source)
                // SY <--
                State.Success(
                    manga = manga,
                    source = source,
                    isFromSource = isFromSource,
                    episodes = chapters,
                    // SY -->
                    availableScanlators = if (manga.source == MERGED_SOURCE_ID) {
                        getAvailableScanlators.awaitMerge(mangaId)
                    } else {
                        getAvailableScanlators.await(mangaId)
                    }.toImmutableSet(),
                    // SY <--
                    excludedScanlators = getExcludedScanlators.await(mangaId).toImmutableSet(),
                    isRefreshingData = needRefreshInfo || needRefreshChapter,
                    dialog = null,
                    // SY -->
                    showRecommendationsInOverflow = uiPreferences.recommendsInOverflow().get(),
                    showMergeInOverflow = uiPreferences.mergeInOverflow().get(),
                    showMergeWithAnother = smartSearched,
                    mergedData = mergedData,
                    // SY <--
                )
            }

            // Start observe tracking since it only needs mangaId
            observeTrackers()

            // Fetch info-chapters when needed
            if (screenModelScope.isActive) {
                val fetchFromSourceTasks = listOf(
                    // KMK -->
                    async { syncTrackers() },
                    // KMK <--
                    async { if (needRefreshInfo) fetchMangaFromSource() },
                    async { if (needRefreshChapter) fetchChaptersFromSource() },
                )
                fetchFromSourceTasks.awaitAll()
                // KMK -->
                launch { fetchRelatedMangasFromSource() }
                // KMK <--
            }

            // Initial loading finished
            updateSuccessState { it.copy(isRefreshingData = false) }
        }
    }

    // KMK -->
    /**
     * Get the color of the manga cover by loading cover with ImageRequest directly from network.
     */
    fun setPaletteColor(model: Any) {
        if (model is ImageRequest && model.defined.sizeResolver != null) return

        val imageRequestBuilder = if (model is ImageRequest) {
            model.newBuilder()
        } else {
            ImageRequest.Builder(context).data(model)
        }
            .allowHardware(false)

        val generatePalette: (Image) -> Unit = { image ->
            val bitmap = image.asDrawable(context.resources).getBitmapOrNull()
            if (bitmap != null) {
                Palette.from(bitmap).generate {
                    screenModelScope.launchIO {
                        if (it == null) return@launchIO
                        val mangaCover = when (model) {
                            is Manga -> model.asMangaCover()
                            is MangaCover -> model
                            else -> return@launchIO
                        }
                        if (mangaCover.isMangaFavorite) {
                            it.dominantSwatch?.let { swatch ->
                                mangaCover.dominantCoverColors = swatch.rgb to swatch.titleTextColor
                            }
                        }
                        val vibrantColor = it.getBestColor() ?: return@launchIO
                        mangaCover.vibrantCoverColor = vibrantColor
                        updateSuccessState {
                            it.copy(seedColor = Color(vibrantColor))
                        }
                    }
                }
            }
        }

        context.imageLoader.enqueue(
            imageRequestBuilder
                .target(
                    onSuccess = generatePalette,
                    onError = {
                        // TODO: handle error
                        // val file = coverCache.getCoverFile(manga!!)
                        // if (file.exists()) {
                        //     file.delete()
                        //     setPaletteColor()
                        // }
                    },
                )
                .build(),
        )
    }

    private suspend fun syncTrackers() {
        if (!trackPreferences.autoSyncProgressFromTrackers().get()) return

        val refreshTracks = Injekt.get<RefreshTracks>()
        refreshTracks.await(mangaId)
            .filter { it.first != null }
            .forEach { (track, e) ->
                logcat(LogPriority.ERROR, e) {
                    "Failed to refresh track data mangaId=$mangaId for service ${track!!.id}"
                }
                withUIContext {
                    context.toast(
                        context.stringResource(
                            MR.strings.track_error,
                            track!!.name,
                            e.message ?: "",
                        ),
                    )
                }
            }
    }
    // KMK <--

    fun fetchAllFromSource(manualFetch: Boolean = true) {
        screenModelScope.launch {
            updateSuccessState { it.copy(isRefreshingData = true) }
            val fetchFromSourceTasks = listOf(
                // KMK -->
                async { syncTrackers() },
                // KMK <--
                async { fetchMangaFromSource(manualFetch) },
                async { fetchChaptersFromSource(manualFetch) },
            )
            fetchFromSourceTasks.awaitAll()
            updateSuccessState { it.copy(isRefreshingData = false) }
        }
    }

    // Manga info - start

    /**
     * Fetch manga information from source.
     */
    private suspend fun fetchMangaFromSource(manualFetch: Boolean = false) {
        val state = successState ?: return
        try {
            withIOContext {
                val networkManga = state.source.getMangaDetails(state.manga.toSAnime())
                updateAnime.awaitUpdateFromSource(state.manga, networkManga, manualFetch)
            }
        } catch (e: Throwable) {
            // Ignore early hints "errors" that aren't handled by OkHttp
            if (e is HttpException && e.code == 103) return

            logcat(LogPriority.ERROR, e)
            screenModelScope.launch {
                snackbarHostState.showSnackbar(message = with(context) { e.formattedMessage })
            }
        }
    }

    // SY -->
    fun updateMangaInfo(
        title: String?,
        author: String?,
        artist: String?,
        thumbnailUrl: String?,
        description: String?,
        tags: List<String>?,
        status: Long?,
    ) {
        val state = successState ?: return
        var manga = state.manga
        if (state.manga.isLocal()) {
            val newTitle = if (title.isNullOrBlank()) manga.url else title.trim()
            val newAuthor = author?.trimOrNull()
            val newArtist = artist?.trimOrNull()
            val newThumbnailUrl = thumbnailUrl?.trimOrNull()
            val newDesc = description?.trimOrNull()
            manga = manga.copy(
                ogTitle = newTitle,
                ogAuthor = author?.trimOrNull(),
                ogArtist = artist?.trimOrNull(),
                ogThumbnailUrl = thumbnailUrl?.trimOrNull(),
                ogDescription = description?.trimOrNull(),
                ogGenre = tags?.nullIfEmpty(),
                ogStatus = status ?: 0,
                lastUpdate = manga.lastUpdate + 1,
            )
            (sourceManager.get(LocalSource.ID) as LocalSource).updateMangaInfo(manga.toSAnime())
            screenModelScope.launchNonCancellable {
                updateAnime.await(
                    MangaUpdate(
                        manga.id,
                        title = newTitle,
                        author = newAuthor,
                        artist = newArtist,
                        thumbnailUrl = newThumbnailUrl,
                        description = newDesc,
                        genre = tags,
                        status = status,
                    ),
                )
            }
        } else {
            val genre = if (!tags.isNullOrEmpty() && tags != state.manga.ogGenre) {
                tags
            } else {
                null
            }
            setCustomAnimeInfo.set(
                CustomMangaInfo(
                    state.manga.id,
                    title?.trimOrNull(),
                    author?.trimOrNull(),
                    artist?.trimOrNull(),
                    thumbnailUrl?.trimOrNull(),
                    description?.trimOrNull(),
                    genre,
                    status.takeUnless { it == state.manga.ogStatus },
                ),
            )
            manga = manga.copy(lastUpdate = manga.lastUpdate + 1)
        }

        updateSuccessState { successState ->
            successState.copy(manga = manga)
        }
    }

    // KMK -->
    @Composable
    fun getManga(initialManga: Manga): RuntimeState<Manga> {
        return produceState(initialValue = initialManga) {
            getAnime.subscribe(initialManga.url, initialManga.source)
                .flowWithLifecycle(lifecycle)
                .collectLatest { manga ->
                    value = manga
                        // KMK -->
                        ?: initialManga
                    // KMK <--
                }
        }
    }

    suspend fun smartSearchMerge(manga: Manga, originalMangaId: Long): Manga {
        return smartSearchMerge.smartSearchMerge(manga, originalMangaId)
    }
    // KMK <--

    fun updateMergeSettings(mergedMangaReferences: List<MergedMangaReference>) {
        screenModelScope.launchNonCancellable {
            if (mergedMangaReferences.isNotEmpty()) {
                updateMergedSettings.awaitAll(
                    mergedMangaReferences.map {
                        MergeMangaSettingsUpdate(
                            id = it.id,
                            isInfoAnime = it.isInfoManga,
                            getEpisodeUpdates = it.getChapterUpdates,
                            episodePriority = it.chapterPriority,
                            downloadEpisodes = it.downloadChapters,
                            episodeSortMode = it.chapterSortMode,
                        )
                    },
                )
            }
        }
    }

    fun deleteMerge(reference: MergedMangaReference) {
        screenModelScope.launchNonCancellable {
            deleteMergeById.await(reference.id)
        }
    }
    // SY <--

    fun toggleFavorite() {
        toggleFavorite(
            onRemoved = {
                screenModelScope.launch {
                    if (!hasDownloads()) return@launch
                    val result = snackbarHostState.showSnackbar(
                        message = context.stringResource(MR.strings.delete_downloads_for_manga),
                        actionLabel = context.stringResource(MR.strings.action_delete),
                        withDismissAction = true,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        deleteDownloads()
                    }
                }
            },
        )
    }

    /**
     * Update favorite status of manga, (removes / adds) manga (to / from) library.
     */
    fun toggleFavorite(
        onRemoved: () -> Unit,
        checkDuplicate: Boolean = true,
    ) {
        val state = successState ?: return
        screenModelScope.launchIO {
            val manga = state.manga

            if (isFavorited) {
                // Remove from library
                if (updateAnime.awaitUpdateFavorite(manga.id, false)) {
                    // Remove covers and update last modified in db
                    if (manga.removeCovers() != manga) {
                        updateAnime.awaitUpdateCoverLastModified(manga.id)
                    }
                    withUIContext { onRemoved() }
                }
            } else {
                // Add to library
                // First, check if duplicate exists if callback is provided
                if (checkDuplicate) {
                    val duplicate = getDuplicateLibraryAnime.await(manga).getOrNull(0)

                    if (duplicate != null) {
                        updateSuccessState { it.copy(dialog = Dialog.DuplicateManga(manga, duplicate)) }
                        return@launchIO
                    }
                }

                // Now check if user previously set categories, when available
                val categories = getCategories()
                val defaultCategoryId = libraryPreferences.defaultCategory().get().toLong()
                val defaultCategory = categories.find { it.id == defaultCategoryId }
                when {
                    // Default category set
                    defaultCategory != null -> {
                        val result = updateAnime.awaitUpdateFavorite(manga.id, true)
                        if (!result) return@launchIO
                        moveMangaToCategory(defaultCategory)
                    }

                    // Automatic 'Default' or no categories
                    defaultCategoryId == 0L || categories.isEmpty() -> {
                        val result = updateAnime.awaitUpdateFavorite(manga.id, true)
                        if (!result) return@launchIO
                        moveMangaToCategory(null)
                    }

                    // Choose a category
                    else -> showChangeCategoryDialog()
                }

                // Finally match with enhanced tracking when available
                addTracks.bindEnhancedTrackers(manga, state.source)
            }
        }
    }

    fun showChangeCategoryDialog() {
        val manga = successState?.manga ?: return
        screenModelScope.launch {
            val categories = getCategories()
            val selection = getMangaCategoryIds(manga)
            updateSuccessState { successState ->
                successState.copy(
                    dialog = Dialog.ChangeCategory(
                        manga = manga,
                        initialSelection = categories.mapAsCheckboxState { it.id in selection }.toImmutableList(),
                    ),
                )
            }
        }
    }

    fun showSetFetchIntervalDialog() {
        val manga = successState?.manga ?: return
        updateSuccessState {
            it.copy(dialog = Dialog.SetFetchInterval(manga))
        }
    }

    fun setFetchInterval(manga: Manga, interval: Int) {
        screenModelScope.launchIO {
            if (
                updateAnime.awaitUpdateFetchInterval(
                    // Custom intervals are negative
                    manga.copy(fetchInterval = -interval),
                )
            ) {
                val updatedManga = mangaRepository.getMangaById(manga.id)
                updateSuccessState { it.copy(manga = updatedManga) }
            }
        }
    }

    /**
     * Returns true if the manga has any downloads.
     */
    private fun hasDownloads(): Boolean {
        val manga = successState?.manga ?: return false
        return downloadManager.getDownloadCount(manga) > 0
    }

    /**
     * Deletes all the downloads for the manga.
     */
    private fun deleteDownloads() {
        val state = successState ?: return
        // SY -->
        if (state.source is MergedSource) {
            val mergedManga = state.mergedData?.manga?.map { it.value to sourceManager.getOrStub(it.value.source) }
            mergedManga?.forEach { (manga, source) ->
                downloadManager.deleteManga(manga, source)
            }
        } else {
            /* SY <-- */ downloadManager.deleteManga(state.manga, state.source)
        }
    }

    /**
     * Get user categories.
     *
     * @return List of categories, not including the default category
     */
    suspend fun getCategories(): List<Category> {
        return getCategories.await().filterNot { it.isSystemCategory }
    }

    /**
     * Gets the category id's the manga is in, if the manga is not in a category, returns the default id.
     *
     * @param manga the manga to get categories from.
     * @return Array of category ids the manga is in, if none returns default id
     */
    private suspend fun getMangaCategoryIds(manga: Manga): List<Long> {
        return getCategories.await(manga.id)
            .map { it.id }
    }

    fun moveMangaToCategoriesAndAddToLibrary(manga: Manga, categories: List<Long>) {
        moveMangaToCategory(categories)
        if (manga.favorite) return

        screenModelScope.launchIO {
            updateAnime.awaitUpdateFavorite(manga.id, true)
        }
    }

    /**
     * Move the given manga to categories.
     *
     * @param categories the selected categories.
     */
    private fun moveMangaToCategories(categories: List<Category>) {
        val categoryIds = categories.map { it.id }
        moveMangaToCategory(categoryIds)
    }

    private fun moveMangaToCategory(categoryIds: List<Long>) {
        screenModelScope.launchIO {
            setMangaCategories.await(mangaId, categoryIds)
        }
    }

    /**
     * Move the given manga to the category.
     *
     * @param category the selected category, or null for default category.
     */
    private fun moveMangaToCategory(category: Category?) {
        moveMangaToCategories(listOfNotNull(category))
    }

    // Manga info - end

    // Chapters list - start

    private fun observeDownloads() {
        // SY -->
        val isMergedSource = source is MergedSource
        val mergedIds = if (isMergedSource) successState?.mergedData?.manga?.keys.orEmpty() else emptySet()
        // SY <--
        screenModelScope.launchIO {
            downloadManager.statusFlow()
                .filter {
                    /* SY --> */ if (isMergedSource) {
                        it.manga.id in mergedIds
                    } else {
                        /* SY <-- */ it.manga.id ==
                            successState?.manga?.id
                    }
                }
                .catch { error -> logcat(LogPriority.ERROR, error) }
                .flowWithLifecycle(lifecycle)
                .collect {
                    withUIContext {
                        updateDownloadState(it)
                    }
                }
        }

        screenModelScope.launchIO {
            downloadManager.progressFlow()
                .filter {
                    /* SY --> */ if (isMergedSource) {
                        it.manga.id in mergedIds
                    } else {
                        /* SY <-- */ it.manga.id ==
                            successState?.manga?.id
                    }
                }
                .catch { error -> logcat(LogPriority.ERROR, error) }
                .flowWithLifecycle(lifecycle)
                .collect {
                    withUIContext {
                        updateDownloadState(it)
                    }
                }
        }
    }

    private fun updateDownloadState(download: Download) {
        updateSuccessState { successState ->
            val modifiedIndex = successState.episodes.indexOfFirst { it.id == download.chapter.id }
            if (modifiedIndex < 0) return@updateSuccessState successState

            val newChapters = successState.episodes.toMutableList().apply {
                val item = removeAt(modifiedIndex)
                    .copy(downloadState = download.status, downloadProgress = download.progress)
                add(modifiedIndex, item)
            }
            successState.copy(episodes = newChapters)
        }
    }

    private fun List<Chapter>.toChapterListItems(
        manga: Manga,
        // SY -->
        mergedData: MergedAnimeData?,
        // SY <--
    ): List<EpisodeList.Item> {
        val isLocal = manga.isLocal()
        return map { chapter ->
            val activeDownload = if (isLocal) {
                null
            } else {
                downloadManager.getQueuedDownloadOrNull(chapter.id)
            }

            // SY -->
            @Suppress("NAME_SHADOWING")
            val manga = mergedData?.manga?.get(chapter.animeId) ?: manga
            val source = mergedData?.sources?.find { manga.source == it.id }?.takeIf { mergedData.sources.size > 2 }
            // SY <--
            val downloaded = if (manga.isLocal()) {
                true
            } else {
                downloadManager.isEpisodeDownloaded(
                    // SY -->
                    chapter.name,
                    chapter.scanlator,
                    manga.ogTitle,
                    manga.source,
                    // SY <--
                )
            }
            val downloadState = when {
                activeDownload != null -> activeDownload.status
                downloaded -> Download.State.DOWNLOADED
                else -> Download.State.NOT_DOWNLOADED
            }

            EpisodeList.Item(
                chapter = chapter,
                downloadState = downloadState,
                downloadProgress = activeDownload?.progress ?: 0,
                selected = chapter.id in selectedChapterIds,
                // SY -->
                sourceName = source?.getNameForAnimeInfo(),
                // SY <--
            )
        }
    }

    /**
     * Requests an updated list of chapters from the source.
     */
    private suspend fun fetchChaptersFromSource(manualFetch: Boolean = false) {
        val state = successState ?: return
        try {
            withIOContext {
                // SY -->
                if (state.source !is MergedSource) {
                    // SY <--
                    val chapters = state.source.getChapterList(state.manga.toSAnime())

                    val newChapters = syncEpisodesWithSource.await(
                        chapters,
                        state.manga,
                        state.source,
                        manualFetch,
                    )

                    if (manualFetch) {
                        downloadNewChapters(newChapters)
                    }
                    // SY -->
                } else {
                    state.source.fetchEpisodesForMergedAnime(state.manga, manualFetch)
                }
                // SY <--
            }
        } catch (e: Throwable) {
            val message = if (e is NoResultsException) {
                context.stringResource(MR.strings.no_chapters_error)
            } else {
                logcat(LogPriority.ERROR, e)
                with(context) { e.formattedMessage }
            }

            screenModelScope.launch {
                snackbarHostState.showSnackbar(message = message)
            }
            val newManga = mangaRepository.getMangaById(mangaId)
            updateSuccessState { it.copy(manga = newManga, isRefreshingData = false) }
        }
    }

    // KMK -->
    /**
     * Set the fetching related mangas status.
     * @param state
     * - false: started & fetching
     * - true: finished
     */
    private fun setRelatedMangasFetchedStatus(state: Boolean) {
        updateSuccessState { it.copy(isRelatedMangasFetched = state) }
    }

    /**
     * Requests an list of related mangas from the source.
     */
    internal suspend fun fetchRelatedMangasFromSource(onDemand: Boolean = false, onFinish: (() -> Unit)? = null) {
        val expandRelatedMangas = uiPreferences.expandRelatedAnimes().get()
        if (!onDemand && !expandRelatedMangas || manga?.source == MERGED_SOURCE_ID) return

        // start fetching related mangas
        setRelatedMangasFetchedStatus(false)

        fun exceptionHandler(e: Throwable) {
            logcat(LogPriority.ERROR, e)
            val message = with(context) { e.formattedMessage }

            screenModelScope.launch {
                snackbarHostState.showSnackbar(message = message)
            }
        }
        val state = successState ?: return
        val relatedMangasEnabled = sourcePreferences.relatedAnimes().get()

        try {
            if (state.source !is StubSource && relatedMangasEnabled) {
                state.source.getRelatedMangaList(state.manga.toSAnime(), { e -> exceptionHandler(e) }) { pair, _ ->
                    /* Push found related mangas into collection */
                    val relatedAnime = RelatedAnime.Success.fromPair(pair) { mangaList ->
                        mangaList.map {
                            // KMK -->
                            it.toDomainAnime(state.source.id)
                            // KMK <--
                        }
                    }

                    updateSuccessState { successState ->
                        val relatedMangaCollection =
                            successState.relatedAnimeCollection
                                ?.toMutableStateList()
                                ?.apply { add(relatedAnime) }
                                ?: listOf(relatedAnime)
                        successState.copy(relatedAnimeCollection = relatedMangaCollection)
                    }
                }
            }
        } catch (e: Exception) {
            exceptionHandler(e)
        } finally {
            if (onFinish != null) {
                onFinish()
            } else {
                setRelatedMangasFetchedStatus(true)
            }
        }
    }
    // KMK <--

    /**
     * @throws IllegalStateException if the swipe action is [LibraryPreferences.EpisodeSwipeAction.Disabled]
     */
    fun chapterSwipe(chapterItem: EpisodeList.Item, swipeAction: LibraryPreferences.EpisodeSwipeAction) {
        screenModelScope.launch {
            executeChapterSwipeAction(chapterItem, swipeAction)
        }
    }

    /**
     * @throws IllegalStateException if the swipe action is [LibraryPreferences.EpisodeSwipeAction.Disabled]
     */
    private fun executeChapterSwipeAction(
        chapterItem: EpisodeList.Item,
        swipeAction: LibraryPreferences.EpisodeSwipeAction,
    ) {
        val chapter = chapterItem.chapter
        when (swipeAction) {
            LibraryPreferences.EpisodeSwipeAction.ToggleSeen -> {
                markEpisodesSeen(listOf(chapter), !chapter.seen)
            }
            LibraryPreferences.EpisodeSwipeAction.ToggleBookmark -> {
                bookmarkChapters(listOf(chapter), !chapter.bookmark)
            }
            LibraryPreferences.EpisodeSwipeAction.Download -> {
                val downloadAction: EpisodeDownloadAction = when (chapterItem.downloadState) {
                    Download.State.ERROR,
                    Download.State.NOT_DOWNLOADED,
                    -> EpisodeDownloadAction.START_NOW
                    Download.State.QUEUE,
                    Download.State.DOWNLOADING,
                    -> EpisodeDownloadAction.CANCEL
                    Download.State.DOWNLOADED -> EpisodeDownloadAction.DELETE
                }
                runChapterDownloadActions(
                    items = listOf(chapterItem),
                    action = downloadAction,
                )
            }
            LibraryPreferences.EpisodeSwipeAction.Disabled -> throw IllegalStateException()
        }
    }

    /**
     * Returns the next unread chapter or null if everything is read.
     */
    fun getNextUnreadChapter(): Chapter? {
        val successState = successState ?: return null
        return successState.episodes.getNextUnseen(successState.manga)
    }

    private fun getUnreadChapters(): List<Chapter> {
        val chapterItems = if (skipFiltered) filteredChapters.orEmpty() else allChapters.orEmpty()
        return chapterItems
            .filter { (chapter, dlStatus) -> !chapter.seen && dlStatus == Download.State.NOT_DOWNLOADED }
            .map { it.chapter }
    }

    private fun getUnreadChaptersSorted(): List<Chapter> {
        val manga = successState?.manga ?: return emptyList()
        val chaptersSorted = getUnreadChapters().sortedWith(getEpisodeSort(manga))
        return if (manga.sortDescending()) chaptersSorted.reversed() else chaptersSorted
    }

    private fun startDownload(
        chapters: List<Chapter>,
        startNow: Boolean,
    ) {
        val successState = successState ?: return

        screenModelScope.launchNonCancellable {
            if (startNow) {
                val chapterId = chapters.singleOrNull()?.id ?: return@launchNonCancellable
                downloadManager.startDownloadNow(chapterId)
            } else {
                downloadChapters(chapters)
            }

            if (!isFavorited && !successState.hasPromptedToAddBefore) {
                updateSuccessState { state ->
                    state.copy(hasPromptedToAddBefore = true)
                }
                val result = snackbarHostState.showSnackbar(
                    message = context.stringResource(MR.strings.snack_add_to_library),
                    actionLabel = context.stringResource(MR.strings.action_add),
                    withDismissAction = true,
                )
                if (result == SnackbarResult.ActionPerformed && !isFavorited) {
                    toggleFavorite()
                }
            }
        }
    }

    fun runChapterDownloadActions(
        items: List<EpisodeList.Item>,
        action: EpisodeDownloadAction,
    ) {
        when (action) {
            EpisodeDownloadAction.START -> {
                startDownload(items.map { it.chapter }, false)
                if (items.any { it.downloadState == Download.State.ERROR }) {
                    downloadManager.startDownloads()
                }
            }
            EpisodeDownloadAction.START_NOW -> {
                val chapter = items.singleOrNull()?.chapter ?: return
                startDownload(listOf(chapter), true)
            }
            EpisodeDownloadAction.CANCEL -> {
                val chapterId = items.singleOrNull()?.id ?: return
                cancelDownload(chapterId)
            }
            EpisodeDownloadAction.DELETE -> {
                deleteChapters(items.map { it.chapter })
            }
        }
    }

    fun runDownloadAction(action: DownloadAction) {
        val chaptersToDownload = when (action) {
            DownloadAction.NEXT_1_EPISODE -> getUnreadChaptersSorted().take(1)
            DownloadAction.NEXT_5_EPISODES -> getUnreadChaptersSorted().take(5)
            DownloadAction.NEXT_10_EPISODES -> getUnreadChaptersSorted().take(10)
            DownloadAction.NEXT_25_EPISODES -> getUnreadChaptersSorted().take(25)
            DownloadAction.UNSEEN_EPISODES -> getUnreadChapters()
        }
        if (chaptersToDownload.isNotEmpty()) {
            startDownload(chaptersToDownload, false)
        }
    }

    private fun cancelDownload(chapterId: Long) {
        val activeDownload = downloadManager.getQueuedDownloadOrNull(chapterId) ?: return
        downloadManager.cancelQueuedDownloads(listOf(activeDownload))
        updateDownloadState(activeDownload.apply { status = Download.State.NOT_DOWNLOADED })
    }

    fun markPreviousChapterRead(pointer: Chapter) {
        val manga = successState?.manga ?: return
        val chapters = filteredChapters.orEmpty().map { it.chapter }
        val prevChapters = if (manga.sortDescending()) chapters.asReversed() else chapters
        val pointerPos = prevChapters.indexOf(pointer)
        if (pointerPos != -1) markEpisodesSeen(prevChapters.take(pointerPos), true)
    }

    /**
     * Mark the selected chapter list as seen/unseen.
     * @param chapters the list of selected chapters.
     * @param seen whether to mark chapters as seen or unseen.
     */
    fun markEpisodesSeen(chapters: List<Chapter>, seen: Boolean) {
        toggleAllSelection(false)
        if (chapters.isEmpty()) return
        screenModelScope.launchIO {
            setSeenStatus.await(
                seen = seen,
                chapters = chapters.toTypedArray(),
            )

            if (!seen || successState?.hasLoggedInTrackers == false || autoTrackState == AutoTrackState.NEVER) {
                return@launchIO
            }

            val tracks = getTracks.await(mangaId)
            val maxChapterNumber = chapters.maxOf { it.episodeNumber }
            val shouldPromptTrackingUpdate = tracks.any { track -> maxChapterNumber > track.lastEpisodeSeen }

            if (!shouldPromptTrackingUpdate) return@launchIO
            if (autoTrackState == AutoTrackState.ALWAYS) {
                trackEpisode.await(context, mangaId, maxChapterNumber)
                withUIContext {
                    context.toast(context.stringResource(MR.strings.trackers_updated_summary, maxChapterNumber.toInt()))
                }
                return@launchIO
            }

            val result = snackbarHostState.showSnackbar(
                message = context.stringResource(MR.strings.confirm_tracker_update, maxChapterNumber.toInt()),
                actionLabel = context.stringResource(MR.strings.action_ok),
                duration = SnackbarDuration.Short,
                withDismissAction = true,
            )

            if (result == SnackbarResult.ActionPerformed) {
                trackEpisode.await(context, mangaId, maxChapterNumber)
            }
        }
    }

    /**
     * Downloads the given list of chapters with the manager.
     * @param chapters the list of chapters to download.
     */
    private fun downloadChapters(chapters: List<Chapter>) {
        // SY -->
        val state = successState ?: return
        if (state.source is MergedSource) {
            chapters.groupBy { it.animeId }.forEach { map ->
                val manga = state.mergedData?.manga?.get(map.key) ?: return@forEach
                downloadManager.downloadEpisodes(manga, map.value)
            }
        } else {
            // SY <--
            val manga = state.manga
            downloadManager.downloadEpisodes(manga, chapters)
        }
        toggleAllSelection(false)
    }

    /**
     * Bookmarks the given list of chapters.
     * @param chapters the list of chapters to bookmark.
     */
    fun bookmarkChapters(chapters: List<Chapter>, bookmarked: Boolean) {
        screenModelScope.launchIO {
            chapters
                .filterNot { it.bookmark == bookmarked }
                .map { EpisodeUpdate(id = it.id, bookmark = bookmarked) }
                .let { updateEpisode.awaitAll(it) }
        }
        toggleAllSelection(false)
    }

    /**
     * Deletes the given list of chapter.
     *
     * @param chapters the list of chapters to delete.
     */
    fun deleteChapters(chapters: List<Chapter>) {
        screenModelScope.launchNonCancellable {
            try {
                successState?.let { state ->
                    downloadManager.deleteEpisodes(
                        chapters,
                        state.manga,
                        state.source,
                        // KMK -->
                        ignoreCategoryExclusion = true,
                        // KMK <--
                    )
                }
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e)
            }
        }
    }

    private fun downloadNewChapters(chapters: List<Chapter>) {
        screenModelScope.launchNonCancellable {
            val manga = successState?.manga ?: return@launchNonCancellable
            val chaptersToDownload = filterChaptersForDownload.await(manga, chapters)

            if (chaptersToDownload.isNotEmpty()) {
                downloadChapters(chaptersToDownload)
            }
        }
    }

    /**
     * Sets the read filter and requests an UI update.
     * @param state whether to display only unread chapters or all chapters.
     */
    fun setUnreadFilter(state: TriState) {
        val manga = successState?.manga ?: return

        val flag = when (state) {
            TriState.DISABLED -> Manga.SHOW_ALL
            TriState.ENABLED_IS -> Manga.EPISODE_SHOW_UNSEEN
            TriState.ENABLED_NOT -> Manga.EPISODE_SHOW_SEEN
        }
        screenModelScope.launchNonCancellable {
            setAnimeEpisodeFlags.awaitSetUnreadFilter(manga, flag)
        }
    }

    /**
     * Sets the download filter and requests an UI update.
     * @param state whether to display only downloaded chapters or all chapters.
     */
    fun setDownloadedFilter(state: TriState) {
        val manga = successState?.manga ?: return

        val flag = when (state) {
            TriState.DISABLED -> Manga.SHOW_ALL
            TriState.ENABLED_IS -> Manga.EPISODE_SHOW_DOWNLOADED
            TriState.ENABLED_NOT -> Manga.EPISODE_SHOW_NOT_DOWNLOADED
        }

        screenModelScope.launchNonCancellable {
            setAnimeEpisodeFlags.awaitSetDownloadedFilter(manga, flag)
        }
    }

    /**
     * Sets the bookmark filter and requests an UI update.
     * @param state whether to display only bookmarked chapters or all chapters.
     */
    fun setBookmarkedFilter(state: TriState) {
        val manga = successState?.manga ?: return

        val flag = when (state) {
            TriState.DISABLED -> Manga.SHOW_ALL
            TriState.ENABLED_IS -> Manga.EPISODE_SHOW_BOOKMARKED
            TriState.ENABLED_NOT -> Manga.EPISODE_SHOW_NOT_BOOKMARKED
        }

        screenModelScope.launchNonCancellable {
            setAnimeEpisodeFlags.awaitSetBookmarkFilter(manga, flag)
        }
    }

    /**
     * Sets the active display mode.
     * @param mode the mode to set.
     */
    fun setDisplayMode(mode: Long) {
        val manga = successState?.manga ?: return

        screenModelScope.launchNonCancellable {
            setAnimeEpisodeFlags.awaitSetDisplayMode(manga, mode)
        }
    }

    /**
     * Sets the sorting method and requests an UI update.
     * @param sort the sorting mode.
     */
    fun setSorting(sort: Long) {
        val manga = successState?.manga ?: return

        screenModelScope.launchNonCancellable {
            setAnimeEpisodeFlags.awaitSetSortingModeOrFlipOrder(manga, sort)
        }
    }

    fun setCurrentSettingsAsDefault(applyToExisting: Boolean) {
        val manga = successState?.manga ?: return
        screenModelScope.launchNonCancellable {
            libraryPreferences.setChapterSettingsDefault(manga)
            if (applyToExisting) {
                setAnimeDefaultEpisodeFlags.awaitAll()
            }
            snackbarHostState.showSnackbar(message = context.stringResource(MR.strings.chapter_settings_updated))
        }
    }

    fun resetToDefaultSettings() {
        val manga = successState?.manga ?: return
        screenModelScope.launchNonCancellable {
            setAnimeDefaultEpisodeFlags.await(manga)
        }
    }

    fun toggleSelection(
        item: EpisodeList.Item,
        selected: Boolean,
        userSelected: Boolean = false,
        fromLongPress: Boolean = false,
    ) {
        updateSuccessState { successState ->
            val newChapters = successState.processedEpisodes.toMutableList().apply {
                val selectedIndex = successState.processedEpisodes.indexOfFirst { it.id == item.chapter.id }
                if (selectedIndex < 0) return@apply

                val selectedItem = get(selectedIndex)
                if ((selectedItem.selected && selected) || (!selectedItem.selected && !selected)) return@apply

                val firstSelection = none { it.selected }
                set(selectedIndex, selectedItem.copy(selected = selected))
                selectedChapterIds.addOrRemove(item.id, selected)

                if (selected && userSelected && fromLongPress) {
                    if (firstSelection) {
                        selectedPositions[0] = selectedIndex
                        selectedPositions[1] = selectedIndex
                    } else {
                        // Try to select the items in-between when possible
                        val range: IntRange
                        if (selectedIndex < selectedPositions[0]) {
                            range = selectedIndex + 1..<selectedPositions[0]
                            selectedPositions[0] = selectedIndex
                        } else if (selectedIndex > selectedPositions[1]) {
                            range = (selectedPositions[1] + 1)..<selectedIndex
                            selectedPositions[1] = selectedIndex
                        } else {
                            // Just select itself
                            range = IntRange.EMPTY
                        }

                        range.forEach {
                            val inbetweenItem = get(it)
                            if (!inbetweenItem.selected) {
                                selectedChapterIds.add(inbetweenItem.id)
                                set(it, inbetweenItem.copy(selected = true))
                            }
                        }
                    }
                } else if (userSelected && !fromLongPress) {
                    if (!selected) {
                        if (selectedIndex == selectedPositions[0]) {
                            selectedPositions[0] = indexOfFirst { it.selected }
                        } else if (selectedIndex == selectedPositions[1]) {
                            selectedPositions[1] = indexOfLast { it.selected }
                        }
                    } else {
                        if (selectedIndex < selectedPositions[0]) {
                            selectedPositions[0] = selectedIndex
                        } else if (selectedIndex > selectedPositions[1]) {
                            selectedPositions[1] = selectedIndex
                        }
                    }
                }
            }
            successState.copy(episodes = newChapters)
        }
    }

    fun toggleAllSelection(selected: Boolean) {
        updateSuccessState { successState ->
            val newChapters = successState.episodes.map {
                selectedChapterIds.addOrRemove(it.id, selected)
                it.copy(selected = selected)
            }
            selectedPositions[0] = -1
            selectedPositions[1] = -1
            successState.copy(episodes = newChapters)
        }
    }

    fun invertSelection() {
        updateSuccessState { successState ->
            val newChapters = successState.episodes.map {
                selectedChapterIds.addOrRemove(it.id, !it.selected)
                it.copy(selected = !it.selected)
            }
            selectedPositions[0] = -1
            selectedPositions[1] = -1
            successState.copy(episodes = newChapters)
        }
    }

    // Chapters list - end

    // Track sheet - start

    private fun observeTrackers() {
        val state = successState
        val manga = state?.manga ?: return

        screenModelScope.launchIO {
            combine(
                getTracks.subscribe(manga.id).catch { logcat(LogPriority.ERROR, it) },
                trackerManager.loggedInTrackersFlow(),
            ) { mangaTracks, loggedInTrackers ->
                // Show only if the service supports this manga's source
                val supportedTrackers = loggedInTrackers.filter { (it as? EnhancedTracker)?.accept(source!!) ?: true }
                val supportedTrackerIds = supportedTrackers.map { it.id }.toHashSet()
                val supportedTrackerTracks = mangaTracks.filter { it.trackerId in supportedTrackerIds }
                supportedTrackerTracks.size to supportedTrackers.isNotEmpty()
            }
                .flowWithLifecycle(lifecycle)
                .distinctUntilChanged()
                .collectLatest { (trackingCount, hasLoggedInTrackers) ->
                    updateSuccessState {
                        it.copy(
                            trackingCount = trackingCount,
                            hasLoggedInTrackers = hasLoggedInTrackers,
                        )
                    }
                }
        }
    }

    // Track sheet - end

    sealed interface Dialog {
        data class ChangeCategory(
            val manga: Manga,
            val initialSelection: ImmutableList<CheckboxState<Category>>,
        ) : Dialog
        data class DeleteChapters(val chapters: List<Chapter>) : Dialog
        data class DuplicateManga(val manga: Manga, val duplicate: Manga) : Dialog

        /* SY -->
        data class Migrate(val newManga: Manga, val oldManga: Manga) : Dialog
        SY <-- */
        data class SetFetchInterval(val manga: Manga) : Dialog

        // SY -->
        data class EditMangaInfo(val manga: Manga) : Dialog
        data class EditMergedSettings(val mergedData: MergedAnimeData) : Dialog
        // SY <--

        data object SettingsSheet : Dialog
        data object TrackSheet : Dialog
        data object FullCover : Dialog
    }

    fun dismissDialog() {
        updateSuccessState { it.copy(dialog = null) }
    }

    fun showDeleteChapterDialog(chapters: List<Chapter>) {
        updateSuccessState { it.copy(dialog = Dialog.DeleteChapters(chapters)) }
    }

    fun showSettingsDialog() {
        updateSuccessState { it.copy(dialog = Dialog.SettingsSheet) }
    }

    fun showTrackDialog() {
        updateSuccessState { it.copy(dialog = Dialog.TrackSheet) }
    }

    fun showCoverDialog() {
        updateSuccessState { it.copy(dialog = Dialog.FullCover) }
    }

    /* SY -->
    fun showMigrateDialog(duplicate: Manga) {
        val manga = successState?.manga ?: return
        updateSuccessState { it.copy(dialog = Dialog.Migrate(newManga = manga, oldManga = duplicate)) }
    } SY <-- */

    fun setExcludedScanlators(excludedScanlators: Set<String>) {
        screenModelScope.launchIO {
            setExcludedScanlators.await(mangaId, excludedScanlators)
        }
    }

    // SY -->
    fun showEditMangaInfoDialog() {
        mutableState.update { state ->
            when (state) {
                State.Loading -> state
                is State.Success -> {
                    state.copy(dialog = Dialog.EditMangaInfo(state.manga))
                }
            }
        }
    }

    fun showEditMergedSettingsDialog() {
        val mergedData = successState?.mergedData ?: return
        mutableState.update { state ->
            when (state) {
                State.Loading -> state
                is State.Success -> {
                    state.copy(dialog = Dialog.EditMergedSettings(mergedData))
                }
            }
        }
    }
    // SY <--

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data class Success(
            val manga: Manga,
            val source: Source,
            val isFromSource: Boolean,
            val episodes: List<EpisodeList.Item>,
            val availableScanlators: ImmutableSet<String>,
            val excludedScanlators: ImmutableSet<String>,
            val trackingCount: Int = 0,
            val hasLoggedInTrackers: Boolean = false,
            val isRefreshingData: Boolean = false,
            val dialog: Dialog? = null,
            val hasPromptedToAddBefore: Boolean = false,

            // SY -->
            val mergedData: MergedAnimeData?,
            val showRecommendationsInOverflow: Boolean,
            val showMergeInOverflow: Boolean,
            val showMergeWithAnother: Boolean,
            // SY <--
            // KMK -->
            /**
             * status of fetching related mangas
             * - null: not started
             * - false: started & fetching
             * - true: finished
             */
            val isRelatedMangasFetched: Boolean? = null,
            /**
             * a list of <keyword, related mangas>
             */
            val relatedAnimeCollection: List<RelatedAnime>? = null,
            val seedColor: Color? = manga.asMangaCover().vibrantCoverColor?.let { Color(it) },
            // KMK <--
        ) : State {
            // KMK -->
            /**
             * a value of null will be treated as still loading, so if all searching were failed and won't update
             * 'relatedAnimeCollection` then we should return empty list
             */
            val relatedAnimesSorted = relatedAnimeCollection
                ?.sorted(manga)
                ?.removeDuplicates(manga)
                ?.filter { it.isVisible() }
                ?.isLoading(isRelatedMangasFetched)
                ?: if (isRelatedMangasFetched == true) emptyList() else null
            // KMK <--

            val processedEpisodes by lazy {
                episodes.applyFilters(manga).toList()
                    // KMK -->
                    // safe-guard some edge-cases where chapters are duplicated some how on a merged entry
                    .distinctBy { it.id }
                // KMK <--
            }

            val isAnySelected by lazy {
                episodes.fastAny { it.selected }
            }

            val episodeListItems by lazy {
                processedEpisodes.insertSeparators { before, after ->
                    val (lowerChapter, higherChapter) = if (manga.sortDescending()) {
                        after to before
                    } else {
                        before to after
                    }
                    if (higherChapter == null) return@insertSeparators null

                    if (lowerChapter == null) {
                        floor(higherChapter.chapter.episodeNumber)
                            .toInt()
                            .minus(1)
                            .coerceAtLeast(0)
                    } else {
                        calculateEpisodeGap(higherChapter.chapter, lowerChapter.chapter)
                    }
                        .takeIf { it > 0 }
                        ?.let { missingCount ->
                            EpisodeList.MissingCount(
                                id = "${lowerChapter?.id}-${higherChapter.id}",
                                count = missingCount,
                            )
                        }
                }
            }

            val scanlatorFilterActive: Boolean
                get() = excludedScanlators.intersect(availableScanlators).isNotEmpty()

            val filterActive: Boolean
                get() = scanlatorFilterActive || manga.chaptersFiltered()

            /**
             * Applies the view filters to the list of chapters obtained from the database.
             * @return an observable of the list of chapters filtered and sorted.
             */
            private fun List<EpisodeList.Item>.applyFilters(manga: Manga): Sequence<EpisodeList.Item> {
                val isLocalManga = manga.isLocal()
                val unreadFilter = manga.unseenFilter
                val downloadedFilter = manga.downloadedFilter
                val bookmarkedFilter = manga.bookmarkedFilter
                return asSequence()
                    .filter { (chapter) -> applyFilter(unreadFilter) { !chapter.seen } }
                    .filter { (chapter) -> applyFilter(bookmarkedFilter) { chapter.bookmark } }
                    .filter { applyFilter(downloadedFilter) { it.isDownloaded || isLocalManga } }
                    .sortedWith { (chapter1), (chapter2) -> getEpisodeSort(manga).invoke(chapter1, chapter2) }
            }
        }
    }
}

// SY -->
data class MergedAnimeData(
    val references: List<MergedMangaReference>,
    val manga: Map<Long, Manga>,
    val sources: List<Source>,
)
// SY <--

@Immutable
sealed class EpisodeList {
    @Immutable
    data class MissingCount(
        val id: String,
        val count: Int,
    ) : EpisodeList()

    @Immutable
    data class Item(
        val chapter: Chapter,
        val downloadState: Download.State,
        val downloadProgress: Int,
        val selected: Boolean = false,
        // SY -->
        val sourceName: String?,
        // SY <--
    ) : EpisodeList() {
        val id = chapter.id
        val isDownloaded = downloadState == Download.State.DOWNLOADED
    }
}

// KMK -->
sealed interface RelatedAnime {
    data object Loading : RelatedAnime

    data class Success(
        val keyword: String,
        val mangaList: List<Manga>,
    ) : RelatedAnime {
        val isEmpty: Boolean
            get() = mangaList.isEmpty()

        companion object {
            suspend fun fromPair(
                pair: Pair<String, List<SManga>>,
                toManga: suspend (mangaList: List<SManga>) -> List<Manga>,
            ) = Success(pair.first, toManga(pair.second))
        }
    }

    fun isVisible(): Boolean {
        return this is Loading || (this is Success && !this.isEmpty)
    }

    companion object {
        internal fun List<RelatedAnime>.sorted(manga: Manga): List<RelatedAnime> {
            val success = filterIsInstance<Success>()
            val loading = filterIsInstance<Loading>()
            val title = manga.title.lowercase()
            val ogTitle = manga.ogTitle.lowercase()
            return success.filter { it.keyword.isEmpty() } +
                success.filter { it.keyword.lowercase() == title } +
                success.filter { it.keyword.lowercase() == ogTitle && ogTitle != title } +
                success.filter { it.keyword.isNotEmpty() && it.keyword.lowercase() !in listOf(title, ogTitle) }
                    .sortedByDescending { it.keyword.length }
                    .sortedBy { it.mangaList.size } +
                loading
        }

        internal fun List<RelatedAnime>.removeDuplicates(manga: Manga): List<RelatedAnime> {
            val mangaHashes = HashSet<Int>().apply { add(manga.url.hashCode()) }

            return map { relatedManga ->
                if (relatedManga is Success) {
                    val stripedList = relatedManga.mangaList.mapNotNull {
                        if (!mangaHashes.contains(it.url.hashCode())) {
                            mangaHashes.add(it.url.hashCode())
                            it
                        } else {
                            null
                        }
                    }
                    Success(
                        relatedManga.keyword,
                        stripedList,
                    )
                } else {
                    relatedManga
                }
            }
        }

        internal fun List<RelatedAnime>.isLoading(isRelatedMangaFetched: Boolean?): List<RelatedAnime> {
            return if (isRelatedMangaFetched == false) this + listOf(Loading) else this
        }
    }
}
// KMK <--
