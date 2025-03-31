package eu.kanade.presentation.manga

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.manga.components.AnimeActionRow
import eu.kanade.presentation.manga.components.AnimeBottomActionMenu
import eu.kanade.presentation.manga.components.AnimeEpisodeListItem
import eu.kanade.presentation.manga.components.AnimeInfoBox
import eu.kanade.presentation.manga.components.AnimeInfoButtons
import eu.kanade.presentation.manga.components.AnimeToolbar
import eu.kanade.presentation.manga.components.EpisodeDownloadAction
import eu.kanade.presentation.manga.components.EpisodeHeader
import eu.kanade.presentation.manga.components.ExpandableAnimeDescription
import eu.kanade.presentation.manga.components.MissingEpisodeCountListItem
import eu.kanade.presentation.manga.components.OutlinedButtonWithArrow
import eu.kanade.presentation.manga.components.RelatedAnimesRow
import eu.kanade.presentation.browse.RelatedAnimeTitle
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.util.formatEpisodeNumber
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.getNameForAnimeInfo
import eu.kanade.tachiyomi.ui.anime.AnimeScreenModel
import eu.kanade.tachiyomi.ui.anime.EpisodeList
import eu.kanade.tachiyomi.ui.anime.MergedAnimeData
import eu.kanade.tachiyomi.util.system.copyToClipboard
import exh.source.MERGED_SOURCE_ID
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.service.missingEpisodesCount
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.model.StubSource
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.components.TwoPanelBox
import tachiyomi.presentation.core.components.VerticalFastScroller
import tachiyomi.presentation.core.components.material.ExtendedFloatingActionButton
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.util.shouldExpandFAB
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import kotlin.math.roundToInt

@Composable
fun AnimeScreen(
    state: AnimeScreenModel.State.Success,
    snackbarHostState: SnackbarHostState,
    nextUpdate: Instant?,
    isTabletUi: Boolean,
    episodeSwipeStartAction: LibraryPreferences.EpisodeSwipeAction,
    episodeSwipeEndAction: LibraryPreferences.EpisodeSwipeAction,
    onBackClicked: () -> Unit,
    onEpisodeClicked: (Chapter) -> Unit,
    onDownloadEpisode: ((List<EpisodeList.Item>, EpisodeDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,

    // For tags menu
    onTagSearch: (String) -> Unit,

    onFilterButtonClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueWatching: () -> Unit,
    onSearch: (query: String, global: Boolean) -> Unit,

    // For cover dialog
    onCoverClicked: () -> Unit,

    // For top action menu
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onEditFetchIntervalClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    // SY -->
    onEditInfoClicked: () -> Unit,
    onRecommendClicked: () -> Unit,
    onMergedSettingsClicked: () -> Unit,
    onMergeClicked: () -> Unit,
    onMergeWithAnotherClicked: () -> Unit,
    // SY <--

    // For bottom action menu
    onMultiBookmarkClicked: (List<Chapter>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<Chapter>, markAsSeen: Boolean) -> Unit,
    onMarkPreviousAsSeenClicked: (Chapter) -> Unit,
    onMultiDeleteClicked: (List<Chapter>) -> Unit,

    // For chapter swipe
    onEpisodeSwipe: (EpisodeList.Item, LibraryPreferences.EpisodeSwipeAction) -> Unit,

    // Chapter selection
    onEpisodeSelected: (EpisodeList.Item, Boolean, Boolean, Boolean) -> Unit,
    onAllEpisodeSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,

    // KMK -->
    getMangaState: @Composable (Manga) -> State<Manga>,
    onRelatedAnimesScreenClick: () -> Unit,
    onRelatedAnimeClick: (Manga) -> Unit,
    onRelatedAnimeLongClick: (Manga) -> Unit,
    librarySearch: (query: String) -> Unit,
    onSourceClick: () -> Unit,
    onCoverLoaded: (MangaCover) -> Unit,
    coverRatio: MutableFloatState,
    hazeState: HazeState,
    // KMK <--
) {
    val context = LocalContext.current
    val onCopyTagToClipboard: (tag: String) -> Unit = {
        if (it.isNotEmpty()) {
            context.copyToClipboard(it, it)
        }
    }

    if (!isTabletUi) {
        AnimeScreenSmallImpl(
            state = state,
            snackbarHostState = snackbarHostState,
            nextUpdate = nextUpdate,
            episodeSwipeStartAction = episodeSwipeStartAction,
            episodeSwipeEndAction = episodeSwipeEndAction,
            onBackClicked = onBackClicked,
            onEpisodeClicked = onEpisodeClicked,
            onDownloadEpisode = onDownloadEpisode,
            onAddToLibraryClicked = onAddToLibraryClicked,
            onWebViewClicked = onWebViewClicked,
            onWebViewLongClicked = onWebViewLongClicked,
            onTrackingClicked = onTrackingClicked,
            onTagSearch = onTagSearch,
            onCopyTagToClipboard = onCopyTagToClipboard,
            onFilterClicked = onFilterButtonClicked,
            onRefresh = onRefresh,
            onContinueWatching = onContinueWatching,
            onSearch = onSearch,
            onCoverClicked = onCoverClicked,
            onShareClicked = onShareClicked,
            onDownloadActionClicked = onDownloadActionClicked,
            onEditCategoryClicked = onEditCategoryClicked,
            onEditIntervalClicked = onEditFetchIntervalClicked,
            onMigrateClicked = onMigrateClicked,
            // SY -->
            onEditInfoClicked = onEditInfoClicked,
            onRecommendClicked = onRecommendClicked,
            onMergedSettingsClicked = onMergedSettingsClicked,
            onMergeClicked = onMergeClicked,
            onMergeWithAnotherClicked = onMergeWithAnotherClicked,
            // SY <--
            onMultiBookmarkClicked = onMultiBookmarkClicked,
            onMultiMarkAsSeenClicked = onMultiMarkAsSeenClicked,
            onMarkPreviousAsSeenClicked = onMarkPreviousAsSeenClicked,
            onMultiDeleteClicked = onMultiDeleteClicked,
            onEpisodeSwipe = onEpisodeSwipe,
            onEpisodeSelected = onEpisodeSelected,
            onAllEpisodeSelected = onAllEpisodeSelected,
            onInvertSelection = onInvertSelection,
            // KMK -->
            getMangaState = getMangaState,
            onRelatedAnimesScreenClick = onRelatedAnimesScreenClick,
            onRelatedAnimeClick = onRelatedAnimeClick,
            onRelatedAnimeLongClick = onRelatedAnimeLongClick,
            librarySearch = librarySearch,
            onSourceClick = onSourceClick,
            onCoverLoaded = onCoverLoaded,
            coverRatio = coverRatio,
            hazeState = hazeState,
            // KMK <--
        )
    } else {
        AnimeScreenLargeImpl(
            state = state,
            snackbarHostState = snackbarHostState,
            nextUpdate = nextUpdate,
            episodeSwipeStartAction = episodeSwipeStartAction,
            episodeSwipeEndAction = episodeSwipeEndAction,
            onBackClicked = onBackClicked,
            onEpisodeClicked = onEpisodeClicked,
            onDownloadEpisode = onDownloadEpisode,
            onAddToLibraryClicked = onAddToLibraryClicked,
            onWebViewClicked = onWebViewClicked,
            onWebViewLongClicked = onWebViewLongClicked,
            onTrackingClicked = onTrackingClicked,
            onTagSearch = onTagSearch,
            onCopyTagToClipboard = onCopyTagToClipboard,
            onFilterButtonClicked = onFilterButtonClicked,
            onRefresh = onRefresh,
            onContinueWatching = onContinueWatching,
            onSearch = onSearch,
            onCoverClicked = onCoverClicked,
            onShareClicked = onShareClicked,
            onDownloadActionClicked = onDownloadActionClicked,
            onEditCategoryClicked = onEditCategoryClicked,
            onEditIntervalClicked = onEditFetchIntervalClicked,
            onMigrateClicked = onMigrateClicked,
            // SY -->
            onEditInfoClicked = onEditInfoClicked,
            onRecommendClicked = onRecommendClicked,
            onMergedSettingsClicked = onMergedSettingsClicked,
            onMergeClicked = onMergeClicked,
            onMergeWithAnotherClicked = onMergeWithAnotherClicked,
            // SY <--
            onMultiBookmarkClicked = onMultiBookmarkClicked,
            onMultiMarkAsSeenClicked = onMultiMarkAsSeenClicked,
            onMarkPreviousAsSeenClicked = onMarkPreviousAsSeenClicked,
            onMultiDeleteClicked = onMultiDeleteClicked,
            onEpisodeSwipe = onEpisodeSwipe,
            onEpisodeSelected = onEpisodeSelected,
            onAllEpisodeSelected = onAllEpisodeSelected,
            onInvertSelection = onInvertSelection,
            // KMK -->
            getMangaState = getMangaState,
            onRelatedAnimesScreenClick = onRelatedAnimesScreenClick,
            onRelatedAnimeClick = onRelatedAnimeClick,
            onRelatedAnimeLongClick = onRelatedAnimeLongClick,
            librarySearch = librarySearch,
            onSourceClick = onSourceClick,
            onCoverLoaded = onCoverLoaded,
            coverRatio = coverRatio,
            hazeState = hazeState,
            // KMK <--
        )
    }
}

@Composable
private fun AnimeScreenSmallImpl(
    state: AnimeScreenModel.State.Success,
    snackbarHostState: SnackbarHostState,
    nextUpdate: Instant?,
    episodeSwipeStartAction: LibraryPreferences.EpisodeSwipeAction,
    episodeSwipeEndAction: LibraryPreferences.EpisodeSwipeAction,
    onBackClicked: () -> Unit,
    onEpisodeClicked: (Chapter) -> Unit,
    onDownloadEpisode: ((List<EpisodeList.Item>, EpisodeDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,

    // For tags menu
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,

    onFilterClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueWatching: () -> Unit,
    onSearch: (query: String, global: Boolean) -> Unit,

    // For cover dialog
    onCoverClicked: () -> Unit,

    // For top action menu
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onEditIntervalClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    // SY -->
    onEditInfoClicked: () -> Unit,
    onRecommendClicked: () -> Unit,
    onMergedSettingsClicked: () -> Unit,
    onMergeClicked: () -> Unit,
    onMergeWithAnotherClicked: () -> Unit,
    // SY <--

    // For bottom action menu
    onMultiBookmarkClicked: (List<Chapter>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<Chapter>, markAsSeen: Boolean) -> Unit,
    onMarkPreviousAsSeenClicked: (Chapter) -> Unit,
    onMultiDeleteClicked: (List<Chapter>) -> Unit,

    // For chapter swipe
    onEpisodeSwipe: (EpisodeList.Item, LibraryPreferences.EpisodeSwipeAction) -> Unit,

    // Chapter selection
    onEpisodeSelected: (EpisodeList.Item, Boolean, Boolean, Boolean) -> Unit,
    onAllEpisodeSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,

    // KMK -->
    getMangaState: @Composable ((Manga) -> State<Manga>),
    onRelatedAnimesScreenClick: () -> Unit,
    onRelatedAnimeClick: (Manga) -> Unit,
    onRelatedAnimeLongClick: (Manga) -> Unit,
    librarySearch: (query: String) -> Unit,
    onSourceClick: () -> Unit,
    onCoverLoaded: (MangaCover) -> Unit,
    coverRatio: MutableFloatState,
    hazeState: HazeState,
    // KMK <--
) {
    val episodeListState = rememberLazyListState()

    val (episodes, listItem, isAnySelected) = remember(state) {
        Triple(
            first = state.processedEpisodes,
            second = state.episodeListItems,
            third = state.isAnySelected,
        )
    }
    // KMK -->
    val uiPreferences = Injekt.get<UiPreferences>()
    val relatedAnimesEnabled by Injekt.get<SourcePreferences>().relatedAnimes().collectAsState()
    val expandRelatedAnimes by uiPreferences.expandRelatedAnimes().collectAsState()
    val showRelatedAnimesInOverflow by uiPreferences.relatedAnimesInOverflow().collectAsState()

    var layoutSize by remember { mutableStateOf(IntSize.Zero) }
    var fabSize by remember { mutableStateOf(IntSize.Zero) }
    var positionOnScreen by remember { mutableStateOf(Offset.Zero) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val fabPosition by uiPreferences.watchButtonPosition().collectAsState()
    val watchButtonPosition = uiPreferences.watchButtonPosition()
    // KMK <--

    val internalOnBackPressed = {
        if (isAnySelected) {
            onAllEpisodeSelected(false)
        } else {
            onBackClicked()
        }
    }
    BackHandler(onBack = internalOnBackPressed)

    Scaffold(
        topBar = {
            val selectedEpisodeCount: Int = remember(episodes) {
                episodes.count { it.selected }
            }
            val isFirstItemVisible by remember {
                derivedStateOf { episodeListState.firstVisibleItemIndex == 0 }
            }
            val isFirstItemScrolled by remember {
                derivedStateOf { episodeListState.firstVisibleItemScrollOffset > 0 }
            }
            val animatedTitleAlpha by animateFloatAsState(
                if (!isFirstItemVisible) 1f else 0f,
                label = "Top Bar Title",
            )
            val animatedBgAlpha by animateFloatAsState(
                if (!isFirstItemVisible || isFirstItemScrolled) 1f else 0f,
                label = "Top Bar Background",
            )
            AnimeToolbar(
                title = state.manga.title,
                titleAlphaProvider = { animatedTitleAlpha },
                backgroundAlphaProvider = { animatedBgAlpha },
                hasFilters = state.filterActive,
                onBackClicked = internalOnBackPressed,
                onClickFilter = onFilterClicked,
                onClickShare = onShareClicked,
                onClickDownload = onDownloadActionClicked,
                onClickEditCategory = onEditCategoryClicked,
                onClickRefresh = onRefresh,
                onClickMigrate = onMigrateClicked,
                // SY -->
                onClickEditInfo = onEditInfoClicked.takeIf { state.manga.favorite },
                // KMK -->
                onClickRelatedAnimes = onRelatedAnimesScreenClick.takeIf {
                    !expandRelatedAnimes &&
                        showRelatedAnimesInOverflow &&
                        state.manga.source != MERGED_SOURCE_ID
                },
                // KMK <--
                onClickRecommend = onRecommendClicked.takeIf { state.showRecommendationsInOverflow },
                onClickMergedSettings = onMergedSettingsClicked.takeIf { state.manga.source == MERGED_SOURCE_ID },
                onClickMerge = onMergeClicked.takeIf { state.showMergeInOverflow },
                // SY <--
                actionModeCounter = selectedEpisodeCount,
                onSelectAll = { onAllEpisodeSelected(true) },
                onInvertSelection = { onInvertSelection() },
            )
        },
        bottomBar = {
            val selectedEpisodes = remember(episodes) {
                episodes.filter { it.selected }
            }
            SharedAnimeBottomActionMenu(
                selected = selectedEpisodes,
                onMultiBookmarkClicked = onMultiBookmarkClicked,
                onMultiMarkAsSeenClicked = onMultiMarkAsSeenClicked,
                onMarkPreviousAsSeenClicked = onMarkPreviousAsSeenClicked,
                onDownloadEpisode = onDownloadEpisode,
                onMultiDeleteClicked = onMultiDeleteClicked,
                fillFraction = 1f,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            val isFABVisible = remember(episodes) {
                episodes.fastAny { !it.chapter.seen } && !isAnySelected
            }
            AnimatedVisibility(
                visible = isFABVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                // KMK -->
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .onGloballyPositioned { coordinates ->
                        fabSize = coordinates.size
                        positionOnScreen = coordinates.positionOnScreen()
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (positionOnScreen.x + fabSize.width / 2 >= layoutSize.width / 2) {
                                    watchButtonPosition.set(FabPosition.End.toString())
                                } else {
                                    watchButtonPosition.set(FabPosition.Start.toString())
                                }
                                offsetX = 0f
                            },
                        ) { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount
                        }
                    },
                // KMK <--
            ) {
                ExtendedFloatingActionButton(
                    text = {
                        val isWatching = remember(state.episodes) {
                            state.episodes.fastAny { it.chapter.seen }
                        }
                        Text(
                            text = stringResource(if (isWatching) MR.strings.action_resume else MR.strings.action_start),
                        )
                    },
                    icon = { Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null) },
                    onClick = onContinueWatching,
                    expanded = episodeListState.shouldExpandFAB(),
                    // KMK -->
                    containerColor = MaterialTheme.colorScheme.primary,
                    // KMK <--
                )
            }
        },
        // KMK -->
        floatingActionButtonPosition = if (fabPosition == FabPosition.End.toString()) {
            FabPosition.End
        } else {
            FabPosition.Start
        },
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                layoutSize = coordinates.size
            }
            .haze(
                state = hazeState,
            ),
        // KMK <--
    ) { contentPadding ->
        val topPadding = contentPadding.calculateTopPadding()

        PullRefresh(
            refreshing = state.isRefreshingData,
            onRefresh = onRefresh,
            enabled = !isAnySelected,
            indicatorPadding = PaddingValues(top = topPadding),
        ) {
            val layoutDirection = LocalLayoutDirection.current
            VerticalFastScroller(
                listState = episodeListState,
                topContentPadding = topPadding,
                endContentPadding = contentPadding.calculateEndPadding(layoutDirection),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    state = episodeListState,
                    contentPadding = PaddingValues(
                        start = contentPadding.calculateStartPadding(layoutDirection),
                        end = contentPadding.calculateEndPadding(layoutDirection),
                        bottom = contentPadding.calculateBottomPadding(),
                    ),
                ) {
                    item(
                        key = AnimeScreenItem.INFO_BOX,
                        contentType = AnimeScreenItem.INFO_BOX,
                    ) {
                        AnimeInfoBox(
                            isTabletUi = false,
                            appBarPadding = topPadding,
                            manga = state.manga,
                            sourceName = remember { state.source.getNameForAnimeInfo(state.mergedData?.sources) },
                            isStubSource = remember { state.source is StubSource },
                            onCoverClick = onCoverClicked,
                            doSearch = onSearch,
                            // KMK -->
                            librarySearch = librarySearch,
                            onSourceClick = onSourceClick,
                            onCoverLoaded = onCoverLoaded,
                            coverRatio = coverRatio,
                            // KMK <--
                        )
                    }

                    item(
                        key = AnimeScreenItem.ACTION_ROW,
                        contentType = AnimeScreenItem.ACTION_ROW,
                    ) {
                        AnimeActionRow(
                            favorite = state.manga.favorite,
                            trackingCount = state.trackingCount,
                            nextUpdate = nextUpdate,
                            isUserIntervalMode = state.manga.fetchInterval < 0,
                            onAddToLibraryClicked = onAddToLibraryClicked,
                            onWebViewClicked = onWebViewClicked,
                            onWebViewLongClicked = onWebViewLongClicked,
                            onTrackingClicked = onTrackingClicked,
                            onEditIntervalClicked = onEditIntervalClicked,
                            onEditCategory = onEditCategoryClicked,
                            // SY -->
                            onMergeClicked = onMergeClicked.takeUnless { state.showMergeInOverflow },
                            // SY <--
                            // KMK -->
                            status = state.manga.status,
                            interval = state.manga.fetchInterval,
                            // KMK <--
                        )
                    }

                    item(
                        key = AnimeScreenItem.DESCRIPTION_WITH_TAG,
                        contentType = AnimeScreenItem.DESCRIPTION_WITH_TAG,
                    ) {
                        ExpandableAnimeDescription(
                            defaultExpandState = state.isFromSource && !state.manga.favorite,
                            description = state.manga.description,
                            tagsProvider = { state.manga.genre },
                            onTagSearch = onTagSearch,
                            onCopyTagToClipboard = onCopyTagToClipboard,
                            // SY -->
                            doSearch = onSearch,
                            // SY <--
                        )
                    }

                    // KMK -->
                    if (state.source !is StubSource &&
                        relatedAnimesEnabled &&
                        state.manga.source != MERGED_SOURCE_ID
                    ) {
                        if (expandRelatedAnimes) {
                            if (state.relatedAnimesSorted?.isNotEmpty() != false) {
                                item { HorizontalDivider() }
                                item(
                                    key = AnimeScreenItem.RELATED_ANIMES,
                                    contentType = AnimeScreenItem.RELATED_ANIMES,
                                ) {
                                    Column {
                                        RelatedAnimeTitle(
                                            title = stringResource(KMR.strings.pref_source_related_mangas),
                                            subtitle = null,
                                            onClick = onRelatedAnimesScreenClick,
                                            onLongClick = null,
                                            modifier = Modifier
                                                .padding(horizontal = MaterialTheme.padding.medium),
                                        )
                                        RelatedAnimesRow(
                                            relatedAnimes = state.relatedAnimesSorted,
                                            getMangaState = getMangaState,
                                            onAnimeClick = onRelatedAnimeClick,
                                            onAnimeLongClick = onRelatedAnimeLongClick,
                                        )
                                    }
                                }
                                item { HorizontalDivider() }
                            }
                        } else if (!showRelatedAnimesInOverflow) {
                            item(
                                key = AnimeScreenItem.RELATED_ANIMES,
                                contentType = AnimeScreenItem.RELATED_ANIMES,
                            ) {
                                OutlinedButtonWithArrow(
                                    text = stringResource(KMR.strings.pref_source_related_mangas)
                                        .uppercase(),
                                    onClick = onRelatedAnimesScreenClick,
                                )
                            }
                        }
                    }
                    // KMK <--

                    // SY -->
                    if (!state.showRecommendationsInOverflow || state.showMergeWithAnother) {
                        item(
                            key = AnimeScreenItem.INFO_BUTTONS,
                            contentType = AnimeScreenItem.INFO_BUTTONS,
                        ) {
                            AnimeInfoButtons(
                                showRecommendsButton = !state.showRecommendationsInOverflow,
                                showMergeWithAnotherButton = state.showMergeWithAnother,
                                onRecommendClicked = onRecommendClicked,
                                onMergeWithAnotherClicked = onMergeWithAnotherClicked,
                            )
                        }
                    }
                    // SY <--

                    item(
                        key = AnimeScreenItem.EPISODE_HEADER,
                        contentType = AnimeScreenItem.EPISODE_HEADER,
                    ) {
                        val missingEpisodeCount = remember(episodes) {
                            episodes.map { it.chapter.episodeNumber }.missingEpisodesCount()
                        }
                        EpisodeHeader(
                            enabled = !isAnySelected,
                            episodeCount = episodes.size,
                            missingEpisodeCount = missingEpisodeCount,
                            onClick = onFilterClicked,
                        )
                    }

                    sharedEpisodeItems(
                        manga = state.manga,
                        mergedData = state.mergedData,
                        episodes = listItem,
                        isAnyEpisodeSelected = episodes.fastAny { it.selected },
                        episodeSwipeStartAction = episodeSwipeStartAction,
                        episodeSwipeEndAction = episodeSwipeEndAction,
                        onEpisodeClicked = onEpisodeClicked,
                        onDownloadEpisode = onDownloadEpisode,
                        onEpisodeSelected = onEpisodeSelected,
                        onEpisodeSwipe = onEpisodeSwipe,
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimeScreenLargeImpl(
    state: AnimeScreenModel.State.Success,
    snackbarHostState: SnackbarHostState,
    nextUpdate: Instant?,
    episodeSwipeStartAction: LibraryPreferences.EpisodeSwipeAction,
    episodeSwipeEndAction: LibraryPreferences.EpisodeSwipeAction,
    onBackClicked: () -> Unit,
    onEpisodeClicked: (Chapter) -> Unit,
    onDownloadEpisode: ((List<EpisodeList.Item>, EpisodeDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,

    // For tags menu
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,

    onFilterButtonClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueWatching: () -> Unit,
    onSearch: (query: String, global: Boolean) -> Unit,

    // For cover dialog
    onCoverClicked: () -> Unit,

    // For top action menu
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onEditIntervalClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    // SY -->
    onEditInfoClicked: () -> Unit,
    onRecommendClicked: () -> Unit,
    onMergedSettingsClicked: () -> Unit,
    onMergeClicked: () -> Unit,
    onMergeWithAnotherClicked: () -> Unit,
    // SY <--

    // For bottom action menu
    onMultiBookmarkClicked: (List<Chapter>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<Chapter>, markAsSeen: Boolean) -> Unit,
    onMarkPreviousAsSeenClicked: (Chapter) -> Unit,
    onMultiDeleteClicked: (List<Chapter>) -> Unit,

    // For swipe actions
    onEpisodeSwipe: (EpisodeList.Item, LibraryPreferences.EpisodeSwipeAction) -> Unit,

    // Chapter selection
    onEpisodeSelected: (EpisodeList.Item, Boolean, Boolean, Boolean) -> Unit,
    onAllEpisodeSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,

    // KMK -->
    getMangaState: @Composable ((Manga) -> State<Manga>),
    onRelatedAnimesScreenClick: () -> Unit,
    onRelatedAnimeClick: (Manga) -> Unit,
    onRelatedAnimeLongClick: (Manga) -> Unit,
    librarySearch: (query: String) -> Unit,
    onSourceClick: () -> Unit,
    onCoverLoaded: (MangaCover) -> Unit,
    coverRatio: MutableFloatState,
    hazeState: HazeState,
    // KMK <--
) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current

    val (episodes, listItem, isAnySelected) = remember(state) {
        Triple(
            first = state.processedEpisodes,
            second = state.episodeListItems,
            third = state.isAnySelected,
        )
    }

    // KMK -->
    val uiPreferences = Injekt.get<UiPreferences>()
    val relatedAnimesEnabled by Injekt.get<SourcePreferences>().relatedAnimes().collectAsState()
    val expandRelatedAnimes by uiPreferences.expandRelatedAnimes().collectAsState()
    val showRelatedAnimesInOverflow by uiPreferences.relatedAnimesInOverflow().collectAsState()

    var layoutSize by remember { mutableStateOf(IntSize.Zero) }
    var fabSize by remember { mutableStateOf(IntSize.Zero) }
    var positionOnScreen by remember { mutableStateOf(Offset.Zero) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val fabPosition by uiPreferences.watchButtonPosition().collectAsState()
    val watchButtonPosition = uiPreferences.watchButtonPosition()
    // KMK <--

    val insetPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()
    var topBarHeight by remember { mutableIntStateOf(0) }

    val episodeListState = rememberLazyListState()

    val internalOnBackPressed = {
        if (isAnySelected) {
            onAllEpisodeSelected(false)
        } else {
            onBackClicked()
        }
    }
    BackHandler(onBack = internalOnBackPressed)

    Scaffold(
        topBar = {
            val selectedEpisodeCount = remember(episodes) {
                episodes.count { it.selected }
            }
            AnimeToolbar(
                modifier = Modifier.onSizeChanged { topBarHeight = it.height },
                title = state.manga.title,
                titleAlphaProvider = { if (isAnySelected) 1f else 0f },
                backgroundAlphaProvider = { 1f },
                hasFilters = state.filterActive,
                onBackClicked = internalOnBackPressed,
                onClickFilter = onFilterButtonClicked,
                onClickShare = onShareClicked,
                onClickDownload = onDownloadActionClicked,
                onClickEditCategory = onEditCategoryClicked,
                onClickRefresh = onRefresh,
                onClickMigrate = onMigrateClicked,
                // SY -->
                onClickEditInfo = onEditInfoClicked.takeIf { state.manga.favorite },
                // KMK -->
                onClickRelatedAnimes = onRelatedAnimesScreenClick.takeIf {
                    !expandRelatedAnimes &&
                        showRelatedAnimesInOverflow &&
                        state.manga.source != MERGED_SOURCE_ID
                },
                // KMK <--
                onClickRecommend = onRecommendClicked.takeIf { state.showRecommendationsInOverflow },
                onClickMergedSettings = onMergedSettingsClicked.takeIf { state.manga.source == MERGED_SOURCE_ID },
                onClickMerge = onMergeClicked.takeIf { state.showMergeInOverflow },
                // SY <--
                actionModeCounter = selectedEpisodeCount,
                onSelectAll = { onAllEpisodeSelected(true) },
                onInvertSelection = { onInvertSelection() },
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomEnd,
            ) {
                val selectedEpisodes = remember(episodes) {
                    episodes.filter { it.selected }
                }
                SharedAnimeBottomActionMenu(
                    selected = selectedEpisodes,
                    onMultiBookmarkClicked = onMultiBookmarkClicked,
                    onMultiMarkAsSeenClicked = onMultiMarkAsSeenClicked,
                    onMarkPreviousAsSeenClicked = onMarkPreviousAsSeenClicked,
                    onDownloadEpisode = onDownloadEpisode,
                    onMultiDeleteClicked = onMultiDeleteClicked,
                    fillFraction = 0.5f,
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            val isFABVisible = remember(episodes) {
                episodes.fastAny { !it.chapter.seen } && !isAnySelected
            }
            AnimatedVisibility(
                visible = isFABVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                // KMK -->
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .onGloballyPositioned { coordinates ->
                        fabSize = coordinates.size
                        positionOnScreen = coordinates.positionOnScreen()
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (positionOnScreen.x + fabSize.width / 2 >= layoutSize.width / 2) {
                                    watchButtonPosition.set(FabPosition.End.toString())
                                } else {
                                    watchButtonPosition.set(FabPosition.Start.toString())
                                }
                                offsetX = 0f
                            },
                        ) { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount
                        }
                    },
                // KMK <--
            ) {
                ExtendedFloatingActionButton(
                    text = {
                        val isWatching = remember(state.episodes) {
                            state.episodes.fastAny { it.chapter.seen }
                        }
                        Text(
                            text = stringResource(
                                if (isWatching) MR.strings.action_resume else MR.strings.action_start,
                            ),
                        )
                    },
                    icon = { Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null) },
                    onClick = onContinueWatching,
                    expanded = episodeListState.shouldExpandFAB(),
                    // KMK -->
                    containerColor = MaterialTheme.colorScheme.primary,
                    // KMK <--
                )
            }
        },
        // KMK -->
        floatingActionButtonPosition = if (fabPosition == FabPosition.End.toString()) {
            FabPosition.End
        } else {
            FabPosition.Start
        },
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                layoutSize = coordinates.size
            }
            .haze(
                state = hazeState,
            ),
        // KMK <--
    ) { contentPadding ->
        PullRefresh(
            refreshing = state.isRefreshingData,
            onRefresh = onRefresh,
            enabled = !isAnySelected,
            indicatorPadding = PaddingValues(
                start = insetPadding.calculateStartPadding(layoutDirection),
                top = with(density) { topBarHeight.toDp() },
                end = insetPadding.calculateEndPadding(layoutDirection),
            ),
        ) {
            TwoPanelBox(
                modifier = Modifier.padding(
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    end = contentPadding.calculateEndPadding(layoutDirection),
                ),
                startContent = {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = contentPadding.calculateBottomPadding()),
                    ) {
                        AnimeInfoBox(
                            isTabletUi = true,
                            appBarPadding = contentPadding.calculateTopPadding(),
                            manga = state.manga,
                            sourceName = remember { state.source.getNameForAnimeInfo(state.mergedData?.sources) },
                            isStubSource = remember { state.source is StubSource },
                            onCoverClick = onCoverClicked,
                            doSearch = onSearch,
                            // KMK -->
                            librarySearch = librarySearch,
                            onSourceClick = onSourceClick,
                            onCoverLoaded = onCoverLoaded,
                            coverRatio = coverRatio,
                            // KMK <--
                        )
                        AnimeActionRow(
                            favorite = state.manga.favorite,
                            trackingCount = state.trackingCount,
                            nextUpdate = nextUpdate,
                            isUserIntervalMode = state.manga.fetchInterval < 0,
                            onAddToLibraryClicked = onAddToLibraryClicked,
                            onWebViewClicked = onWebViewClicked,
                            onWebViewLongClicked = onWebViewLongClicked,
                            onTrackingClicked = onTrackingClicked,
                            onEditIntervalClicked = onEditIntervalClicked,
                            onEditCategory = onEditCategoryClicked,
                            // SY -->
                            onMergeClicked = onMergeClicked.takeUnless { state.showMergeInOverflow },
                            // SY <--
                            // KMK -->
                            status = state.manga.status,
                            interval = state.manga.fetchInterval,
                            // KMK <--
                        )
                        ExpandableAnimeDescription(
                            defaultExpandState = true,
                            description = state.manga.description,
                            tagsProvider = { state.manga.genre },
                            onTagSearch = onTagSearch,
                            onCopyTagToClipboard = onCopyTagToClipboard,
                            // SY -->
                            doSearch = onSearch,
                            // SY <--
                        )
                        // SY -->
                        if (!state.showRecommendationsInOverflow || state.showMergeWithAnother) {
                            AnimeInfoButtons(
                                showRecommendsButton = !state.showRecommendationsInOverflow,
                                showMergeWithAnotherButton = state.showMergeWithAnother,
                                onRecommendClicked = onRecommendClicked,
                                onMergeWithAnotherClicked = onMergeWithAnotherClicked,
                            )
                        }
                        // SY <--
                    }
                },
                endContent = {
                    VerticalFastScroller(
                        listState = episodeListState,
                        topContentPadding = contentPadding.calculateTopPadding(),
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxHeight(),
                            state = episodeListState,
                            contentPadding = PaddingValues(
                                top = contentPadding.calculateTopPadding(),
                                bottom = contentPadding.calculateBottomPadding(),
                            ),
                        ) {
                            // KMK -->
                            if (state.source !is StubSource &&
                                relatedAnimesEnabled &&
                                state.manga.source != MERGED_SOURCE_ID
                            ) {
                                if (expandRelatedAnimes) {
                                    if (state.relatedAnimesSorted?.isNotEmpty() != false) {
                                        item(
                                            key = AnimeScreenItem.RELATED_ANIMES,
                                            contentType = AnimeScreenItem.RELATED_ANIMES,
                                        ) {
                                            Column {
                                                RelatedAnimeTitle(
                                                    title = stringResource(KMR.strings.pref_source_related_mangas)
                                                        .uppercase(),
                                                    subtitle = null,
                                                    onClick = onRelatedAnimesScreenClick,
                                                    onLongClick = null,
                                                    modifier = Modifier
                                                        .padding(horizontal = MaterialTheme.padding.medium),
                                                )
                                                RelatedAnimesRow(
                                                    relatedAnimes = state.relatedAnimesSorted,
                                                    getMangaState = getMangaState,
                                                    onAnimeClick = onRelatedAnimeClick,
                                                    onAnimeLongClick = onRelatedAnimeLongClick,
                                                )
                                            }
                                        }
                                        item { HorizontalDivider() }
                                    }
                                } else if (!showRelatedAnimesInOverflow) {
                                    item(
                                        key = AnimeScreenItem.RELATED_ANIMES,
                                        contentType = AnimeScreenItem.RELATED_ANIMES,
                                    ) {
                                        OutlinedButtonWithArrow(
                                            text = stringResource(KMR.strings.pref_source_related_mangas),
                                            onClick = onRelatedAnimesScreenClick,
                                        )
                                    }
                                }
                            }
                            // KMK <--

                            item(
                                key = AnimeScreenItem.EPISODE_HEADER,
                                contentType = AnimeScreenItem.EPISODE_HEADER,
                            ) {
                                val missingEpisodeCount = remember(episodes) {
                                    episodes.map { it.chapter.episodeNumber }.missingEpisodesCount()
                                }
                                EpisodeHeader(
                                    enabled = !isAnySelected,
                                    episodeCount = episodes.size,
                                    missingEpisodeCount = missingEpisodeCount,
                                    onClick = onFilterButtonClicked,
                                )
                            }

                            sharedEpisodeItems(
                                manga = state.manga,
                                mergedData = state.mergedData,
                                episodes = listItem,
                                isAnyEpisodeSelected = episodes.fastAny { it.selected },
                                episodeSwipeStartAction = episodeSwipeStartAction,
                                episodeSwipeEndAction = episodeSwipeEndAction,
                                onEpisodeClicked = onEpisodeClicked,
                                onDownloadEpisode = onDownloadEpisode,
                                onEpisodeSelected = onEpisodeSelected,
                                onEpisodeSwipe = onEpisodeSwipe,
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun SharedAnimeBottomActionMenu(
    selected: List<EpisodeList.Item>,
    onMultiBookmarkClicked: (List<Chapter>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<Chapter>, markAsSeen: Boolean) -> Unit,
    onMarkPreviousAsSeenClicked: (Chapter) -> Unit,
    onDownloadEpisode: ((List<EpisodeList.Item>, EpisodeDownloadAction) -> Unit)?,
    onMultiDeleteClicked: (List<Chapter>) -> Unit,
    fillFraction: Float,
    modifier: Modifier = Modifier,
) {
    AnimeBottomActionMenu(
        visible = selected.isNotEmpty(),
        modifier = modifier.fillMaxWidth(fillFraction),
        onBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected.fastMap { it.chapter }, true)
        }.takeIf { selected.fastAny { !it.chapter.bookmark } },
        onRemoveBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected.fastMap { it.chapter }, false)
        }.takeIf { selected.fastAll { it.chapter.bookmark } },
        onMarkAsSeenClicked = {
            onMultiMarkAsSeenClicked(selected.fastMap { it.chapter }, true)
        }.takeIf { selected.fastAny { !it.chapter.seen } },
        onMarkAsUnseenClicked = {
            onMultiMarkAsSeenClicked(selected.fastMap { it.chapter }, false)
        }.takeIf { selected.fastAny { it.chapter.seen || it.chapter.lastSecondSeen > 0L } },
        onMarkPreviousAsSeenClicked = {
            onMarkPreviousAsSeenClicked(selected[0].chapter)
        }.takeIf { selected.size == 1 },
        onDownloadClicked = {
            onDownloadEpisode!!(selected.toList(), EpisodeDownloadAction.START)
        }.takeIf {
            onDownloadEpisode != null && selected.fastAny { it.downloadState != Download.State.DOWNLOADED }
        },
        onDeleteClicked = {
            onMultiDeleteClicked(selected.fastMap { it.chapter })
        }.takeIf {
            selected.fastAny { it.downloadState == Download.State.DOWNLOADED }
        },
    )
}

private fun LazyListScope.sharedEpisodeItems(
    manga: Manga,
    mergedData: MergedAnimeData?,
    episodes: List<EpisodeList>,
    isAnyEpisodeSelected: Boolean,
    episodeSwipeStartAction: LibraryPreferences.EpisodeSwipeAction,
    episodeSwipeEndAction: LibraryPreferences.EpisodeSwipeAction,
    onEpisodeClicked: (Chapter) -> Unit,
    onDownloadEpisode: ((List<EpisodeList.Item>, EpisodeDownloadAction) -> Unit)?,
    onEpisodeSelected: (EpisodeList.Item, Boolean, Boolean, Boolean) -> Unit,
    onEpisodeSwipe: (EpisodeList.Item, LibraryPreferences.EpisodeSwipeAction) -> Unit,
) {
    items(
        items = episodes,
        key = { item ->
            when (item) {
                // KMK: using hashcode to prevent edge-cases where the missing count might duplicate,
                // especially on merged manga
                is EpisodeList.MissingCount -> "missing-count-${item.hashCode()}"
                is EpisodeList.Item -> "episode-${item.id}"
            }
        },
        contentType = { AnimeScreenItem.EPISODE },
    ) { item ->
        val haptic = LocalHapticFeedback.current

        when (item) {
            is EpisodeList.MissingCount -> {
                MissingEpisodeCountListItem(count = item.count)
            }
            is EpisodeList.Item -> {
                AnimeEpisodeListItem(
                    title = if (manga.displayMode == Manga.EPISODE_DISPLAY_NUMBER) {
                        stringResource(
                            MR.strings.display_mode_chapter,
                            formatEpisodeNumber(item.chapter.episodeNumber),
                        )
                    } else {
                        item.chapter.name
                    },
                    date = item.chapter.dateUpload
                        .takeIf { it > 0L }
                        ?.let {
                            relativeDateText(item.chapter.dateUpload)
                        },
                    watchProgress = item.chapter.lastSecondSeen
                        .takeIf {
                            /* SY --> */(!item.chapter.seen)/* SY <-- */ && it > 0L
                        }
                        ?.let {
                            stringResource(
                                MR.strings.chapter_progress,
                                it + 1,
                            )
                        },
                    scanlator = item.chapter.scanlator.takeIf {
                        !it.isNullOrBlank()
                    },
                    // SY -->
                    sourceName = item.sourceName,
                    // SY <--
                    seen = item.chapter.seen,
                    bookmark = item.chapter.bookmark,
                    selected = item.selected,
                    downloadIndicatorEnabled =
                    !isAnyEpisodeSelected && !(mergedData?.manga?.get(item.chapter.animeId) ?: manga).isLocal(),
                    downloadStateProvider = { item.downloadState },
                    downloadProgressProvider = { item.downloadProgress },
                    episodeSwipeStartAction = episodeSwipeStartAction,
                    episodeSwipeEndAction = episodeSwipeEndAction,
                    onLongClick = {
                        onEpisodeSelected(item, !item.selected, true, true)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onClick = {
                        onEpisodeItemClick(
                            episodeItem = item,
                            isAnyEpisodeSelected = isAnyEpisodeSelected,
                            onToggleSelection = { onEpisodeSelected(item, !item.selected, true, false) },
                            onEpisodeClicked = onEpisodeClicked,
                        )
                    },
                    onDownloadClick = if (onDownloadEpisode != null) {
                        { onDownloadEpisode(listOf(item), it) }
                    } else {
                        null
                    },
                    onEpisodeSwipe = {
                        onEpisodeSwipe(item, it)
                    },
                )
            }
        }
    }
}

private fun onEpisodeItemClick(
    episodeItem: EpisodeList.Item,
    isAnyEpisodeSelected: Boolean,
    onToggleSelection: (Boolean) -> Unit,
    onEpisodeClicked: (Chapter) -> Unit,
) {
    when {
        episodeItem.selected -> onToggleSelection(false)
        isAnyEpisodeSelected -> onToggleSelection(true)
        else -> onEpisodeClicked(episodeItem.chapter)
    }
}
