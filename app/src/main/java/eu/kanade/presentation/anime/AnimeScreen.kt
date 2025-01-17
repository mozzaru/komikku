package eu.kanade.presentation.anime

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.anime.components.AnimeActionRow
import eu.kanade.presentation.anime.components.AnimeBottomActionMenu
import eu.kanade.presentation.anime.components.AnimeChapterListItem
import eu.kanade.presentation.anime.components.AnimeInfoBox
import eu.kanade.presentation.anime.components.AnimeInfoButtons
import eu.kanade.presentation.anime.components.AnimeToolbar
import eu.kanade.presentation.anime.components.EpisodeDownloadAction
import eu.kanade.presentation.anime.components.EpisodeHeader
import eu.kanade.presentation.anime.components.ExpandableAnimeDescription
import eu.kanade.presentation.anime.components.MissingEpisodeCountListItem
import eu.kanade.presentation.anime.components.OutlinedButtonWithArrow
import eu.kanade.presentation.anime.components.PagePreviewItems
import eu.kanade.presentation.anime.components.PagePreviews
import eu.kanade.presentation.anime.components.RelatedAnimesRow
import eu.kanade.presentation.anime.components.SearchMetadataChips
import eu.kanade.presentation.browse.RelatedAnimeTitle
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.util.formatEpisodeNumber
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.animesource.Source
import eu.kanade.tachiyomi.source.getNameForMangaInfo
import eu.kanade.tachiyomi.animesource.online.MetadataSource
import eu.kanade.tachiyomi.source.online.all.EHentai
import eu.kanade.tachiyomi.source.online.all.MangaDex
import eu.kanade.tachiyomi.source.online.all.NHentai
import eu.kanade.tachiyomi.source.online.english.EightMuses
import eu.kanade.tachiyomi.source.online.english.HBrowse
import eu.kanade.tachiyomi.source.online.english.Pururin
import eu.kanade.tachiyomi.source.online.english.Tsumino
import eu.kanade.tachiyomi.ui.anime.AnimeScreenModel
import eu.kanade.tachiyomi.ui.anime.EpisodeList
import eu.kanade.tachiyomi.ui.anime.MergedMangaData
import eu.kanade.tachiyomi.ui.anime.PagePreviewState
import eu.kanade.tachiyomi.util.system.copyToClipboard
import exh.metadata.MetadataUtil
import exh.source.MERGED_SOURCE_ID
import exh.source.getMainSource
import exh.source.isEhBasedManga
import exh.ui.metadata.adapters.EHentaiDescription
import exh.ui.metadata.adapters.EightMusesDescription
import exh.ui.metadata.adapters.HBrowseDescription
import exh.ui.metadata.adapters.MangaDexDescription
import exh.ui.metadata.adapters.NHentaiDescription
import exh.ui.metadata.adapters.PururinDescription
import exh.ui.metadata.adapters.TsuminoDescription
import tachiyomi.domain.anime.model.AnimeCover
import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.episode.service.missingEpisodesCount
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
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.roundToInt

@Composable
fun AnimeScreen(
    state: AnimeScreenModel.State.Success,
    snackbarHostState: SnackbarHostState,
    nextUpdate: Instant?,
    isTabletUi: Boolean,
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    onBackClicked: () -> Unit,
    onChapterClicked: (Episode) -> Unit,
    onDownloadChapter: ((List<EpisodeList.Item>, EpisodeDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,

    // For tags menu
    onTagSearch: (String) -> Unit,

    onFilterButtonClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueReading: () -> Unit,
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
    onMetadataViewerClicked: () -> Unit,
    onEditInfoClicked: () -> Unit,
    onRecommendClicked: () -> Unit,
    onMergedSettingsClicked: () -> Unit,
    onMergeClicked: () -> Unit,
    onMergeWithAnotherClicked: () -> Unit,
    onOpenPagePreview: (Int) -> Unit,
    onMorePreviewsClicked: () -> Unit,
    previewsRowCount: Int,
    // SY <--

    // For bottom action menu
    onMultiBookmarkClicked: (List<Episode>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<Episode>, markAsRead: Boolean) -> Unit,
    onMarkPreviousAsReadClicked: (Episode) -> Unit,
    onMultiDeleteClicked: (List<Episode>) -> Unit,

    // For episode swipe
    onChapterSwipe: (EpisodeList.Item, LibraryPreferences.ChapterSwipeAction) -> Unit,

    // Episode selection
    onChapterSelected: (EpisodeList.Item, Boolean, Boolean, Boolean) -> Unit,
    onAllChapterSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,

    // KMK -->
    getMangaState: @Composable (Manga) -> State<Manga>,
    onRelatedMangasScreenClick: () -> Unit,
    onRelatedMangaClick: (Manga) -> Unit,
    onRelatedMangaLongClick: (Manga) -> Unit,
    librarySearch: (query: String) -> Unit,
    onSourceClick: () -> Unit,
    onCoverLoaded: (AnimeCover) -> Unit,
    coverRatio: MutableFloatState,
    onPaletteScreenClick: () -> Unit,
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
            chapterSwipeStartAction = chapterSwipeStartAction,
            chapterSwipeEndAction = chapterSwipeEndAction,
            onBackClicked = onBackClicked,
            onChapterClicked = onChapterClicked,
            onDownloadChapter = onDownloadChapter,
            onAddToLibraryClicked = onAddToLibraryClicked,
            onWebViewClicked = onWebViewClicked,
            onWebViewLongClicked = onWebViewLongClicked,
            onTrackingClicked = onTrackingClicked,
            onTagSearch = onTagSearch,
            onCopyTagToClipboard = onCopyTagToClipboard,
            onFilterClicked = onFilterButtonClicked,
            onRefresh = onRefresh,
            onContinueReading = onContinueReading,
            onSearch = onSearch,
            onCoverClicked = onCoverClicked,
            onShareClicked = onShareClicked,
            onDownloadActionClicked = onDownloadActionClicked,
            onEditCategoryClicked = onEditCategoryClicked,
            onEditIntervalClicked = onEditFetchIntervalClicked,
            onMigrateClicked = onMigrateClicked,
            // SY -->
            onMetadataViewerClicked = onMetadataViewerClicked,
            onEditInfoClicked = onEditInfoClicked,
            onRecommendClicked = onRecommendClicked,
            onMergedSettingsClicked = onMergedSettingsClicked,
            onMergeClicked = onMergeClicked,
            onMergeWithAnotherClicked = onMergeWithAnotherClicked,
            onOpenPagePreview = onOpenPagePreview,
            onMorePreviewsClicked = onMorePreviewsClicked,
            previewsRowCount = previewsRowCount,
            // SY <--
            onMultiBookmarkClicked = onMultiBookmarkClicked,
            onMultiMarkAsReadClicked = onMultiMarkAsReadClicked,
            onMarkPreviousAsReadClicked = onMarkPreviousAsReadClicked,
            onMultiDeleteClicked = onMultiDeleteClicked,
            onChapterSwipe = onChapterSwipe,
            onChapterSelected = onChapterSelected,
            onAllChapterSelected = onAllChapterSelected,
            onInvertSelection = onInvertSelection,
            // KMK -->
            getMangaState = getMangaState,
            onRelatedMangasScreenClick = onRelatedMangasScreenClick,
            onRelatedMangaClick = onRelatedMangaClick,
            onRelatedMangaLongClick = onRelatedMangaLongClick,
            librarySearch = librarySearch,
            onSourceClick = onSourceClick,
            onCoverLoaded = onCoverLoaded,
            coverRatio = coverRatio,
            onPaletteScreenClick = onPaletteScreenClick,
            hazeState = hazeState,
            // KMK <--
        )
    } else {
        AnimeScreenLargeImpl(
            state = state,
            snackbarHostState = snackbarHostState,
            chapterSwipeStartAction = chapterSwipeStartAction,
            chapterSwipeEndAction = chapterSwipeEndAction,
            nextUpdate = nextUpdate,
            onBackClicked = onBackClicked,
            onChapterClicked = onChapterClicked,
            onDownloadChapter = onDownloadChapter,
            onAddToLibraryClicked = onAddToLibraryClicked,
            onWebViewClicked = onWebViewClicked,
            onWebViewLongClicked = onWebViewLongClicked,
            onTrackingClicked = onTrackingClicked,
            onTagSearch = onTagSearch,
            onCopyTagToClipboard = onCopyTagToClipboard,
            onFilterButtonClicked = onFilterButtonClicked,
            onRefresh = onRefresh,
            onContinueReading = onContinueReading,
            onSearch = onSearch,
            onCoverClicked = onCoverClicked,
            onShareClicked = onShareClicked,
            onDownloadActionClicked = onDownloadActionClicked,
            onEditCategoryClicked = onEditCategoryClicked,
            onEditIntervalClicked = onEditFetchIntervalClicked,
            onMigrateClicked = onMigrateClicked,
            // SY -->
            onMetadataViewerClicked = onMetadataViewerClicked,
            onEditInfoClicked = onEditInfoClicked,
            onRecommendClicked = onRecommendClicked,
            onMergedSettingsClicked = onMergedSettingsClicked,
            onMergeClicked = onMergeClicked,
            onMergeWithAnotherClicked = onMergeWithAnotherClicked,
            onOpenPagePreview = onOpenPagePreview,
            onMorePreviewsClicked = onMorePreviewsClicked,
            previewsRowCount = previewsRowCount,
            // SY <--
            onMultiBookmarkClicked = onMultiBookmarkClicked,
            onMultiMarkAsReadClicked = onMultiMarkAsReadClicked,
            onMarkPreviousAsReadClicked = onMarkPreviousAsReadClicked,
            onMultiDeleteClicked = onMultiDeleteClicked,
            onChapterSwipe = onChapterSwipe,
            onChapterSelected = onChapterSelected,
            onAllChapterSelected = onAllChapterSelected,
            onInvertSelection = onInvertSelection,
            // KMK -->
            getMangaState = getMangaState,
            onRelatedMangasScreenClick = onRelatedMangasScreenClick,
            onRelatedMangaClick = onRelatedMangaClick,
            onRelatedMangaLongClick = onRelatedMangaLongClick,
            librarySearch = librarySearch,
            onSourceClick = onSourceClick,
            onCoverLoaded = onCoverLoaded,
            coverRatio = coverRatio,
            onPaletteScreenClick = onPaletteScreenClick,
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
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    onBackClicked: () -> Unit,
    onChapterClicked: (Episode) -> Unit,
    onDownloadChapter: ((List<EpisodeList.Item>, EpisodeDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,

    // For tags menu
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,

    onFilterClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueReading: () -> Unit,
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
    onMetadataViewerClicked: () -> Unit,
    onEditInfoClicked: () -> Unit,
    onRecommendClicked: () -> Unit,
    onMergedSettingsClicked: () -> Unit,
    onMergeClicked: () -> Unit,
    onMergeWithAnotherClicked: () -> Unit,
    onOpenPagePreview: (Int) -> Unit,
    onMorePreviewsClicked: () -> Unit,
    previewsRowCount: Int,
    // SY <--

    // For bottom action menu
    onMultiBookmarkClicked: (List<Episode>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<Episode>, markAsRead: Boolean) -> Unit,
    onMarkPreviousAsReadClicked: (Episode) -> Unit,
    onMultiDeleteClicked: (List<Episode>) -> Unit,

    // For episode swipe
    onChapterSwipe: (EpisodeList.Item, LibraryPreferences.ChapterSwipeAction) -> Unit,

    // Episode selection
    onChapterSelected: (EpisodeList.Item, Boolean, Boolean, Boolean) -> Unit,
    onAllChapterSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,

    // KMK -->
    getMangaState: @Composable ((Manga) -> State<Manga>),
    onRelatedMangasScreenClick: () -> Unit,
    onRelatedMangaClick: (Manga) -> Unit,
    onRelatedMangaLongClick: (Manga) -> Unit,
    librarySearch: (query: String) -> Unit,
    onSourceClick: () -> Unit,
    onCoverLoaded: (AnimeCover) -> Unit,
    coverRatio: MutableFloatState,
    onPaletteScreenClick: () -> Unit,
    hazeState: HazeState,
    // KMK <--
) {
    val chapterListState = rememberLazyListState()

    val (chapters, listItem, isAnySelected) = remember(state) {
        Triple(
            first = state.processedChapters,
            second = state.episodeListItems,
            third = state.isAnySelected,
        )
    }
    // SY -->
    val metadataDescription = metadataDescription(state.source)
    var maxWidth by remember {
        mutableStateOf(Dp.Hairline)
    }
    // SY <--
    // KMK -->
    val uiPreferences = Injekt.get<UiPreferences>()
    val relatedMangasEnabled by Injekt.get<SourcePreferences>().relatedMangas().collectAsState()
    val expandRelatedMangas by uiPreferences.expandRelatedMangas().collectAsState()
    val showRelatedMangasInOverflow by uiPreferences.relatedMangasInOverflow().collectAsState()

    var layoutSize by remember { mutableStateOf(IntSize.Zero) }
    var fabSize by remember { mutableStateOf(IntSize.Zero) }
    var positionOnScreen by remember { mutableStateOf(Offset.Zero) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val fabPosition by uiPreferences.readButtonPosition().collectAsState()
    val readButtonPosition = uiPreferences.readButtonPosition()
    // KMK <--

    val internalOnBackPressed = {
        if (isAnySelected) {
            onAllChapterSelected(false)
        } else {
            onBackClicked()
        }
    }
    BackHandler(onBack = internalOnBackPressed)

    Scaffold(
        topBar = {
            val selectedChapterCount: Int = remember(chapters) {
                chapters.count { it.selected }
            }
            val isFirstItemVisible by remember {
                derivedStateOf { chapterListState.firstVisibleItemIndex == 0 }
            }
            val isFirstItemScrolled by remember {
                derivedStateOf { chapterListState.firstVisibleItemScrollOffset > 0 }
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
                onClickRelatedMangas = onRelatedMangasScreenClick.takeIf {
                    !expandRelatedMangas &&
                        showRelatedMangasInOverflow &&
                        state.manga.source != MERGED_SOURCE_ID
                },
                // KMK <--
                onClickRecommend = onRecommendClicked.takeIf { state.showRecommendationsInOverflow },
                onClickMergedSettings = onMergedSettingsClicked.takeIf { state.manga.source == MERGED_SOURCE_ID },
                onClickMerge = onMergeClicked.takeIf { state.showMergeInOverflow },
                // SY <--
                actionModeCounter = selectedChapterCount,
                onSelectAll = { onAllChapterSelected(true) },
                onInvertSelection = { onInvertSelection() },
                // KMK -->
                onPaletteScreenClick = onPaletteScreenClick,
                // KMK <--
            )
        },
        bottomBar = {
            val selectedChapters = remember(chapters) {
                chapters.filter { it.selected }
            }
            SharedAnimeBottomActionMenu(
                selected = selectedChapters,
                onMultiBookmarkClicked = onMultiBookmarkClicked,
                onMultiMarkAsReadClicked = onMultiMarkAsReadClicked,
                onMarkPreviousAsReadClicked = onMarkPreviousAsReadClicked,
                onDownloadChapter = onDownloadChapter,
                onMultiDeleteClicked = onMultiDeleteClicked,
                fillFraction = 1f,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            val isFABVisible = remember(chapters) {
                chapters.fastAny { !it.episode.read } && !isAnySelected
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
                                    readButtonPosition.set(FabPosition.End.toString())
                                } else {
                                    readButtonPosition.set(FabPosition.Start.toString())
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
                        val isReading = remember(state.chapters) {
                            state.chapters.fastAny { it.episode.read }
                        }
                        Text(
                            text = stringResource(if (isReading) MR.strings.action_resume else MR.strings.action_start),
                        )
                    },
                    icon = { Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null) },
                    onClick = onContinueReading,
                    expanded = chapterListState.shouldExpandFAB(),
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
                listState = chapterListState,
                topContentPadding = topPadding,
                endContentPadding = contentPadding.calculateEndPadding(layoutDirection),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    state = chapterListState,
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
                            sourceName = remember { state.source.getNameForMangaInfo(state.mergedData?.sources) },
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

                    // SY -->
                    if (metadataDescription != null) {
                        item(
                            key = AnimeScreenItem.METADATA_INFO,
                            contentType = AnimeScreenItem.METADATA_INFO,
                        ) {
                            metadataDescription(
                                state,
                                onMetadataViewerClicked,
                            ) {
                                onSearch(it, false)
                            }
                        }
                    }
                    // SY <--

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
                            searchMetadataChips = remember(state.meta, state.source.id, state.manga.genre) {
                                SearchMetadataChips(state.meta, state.source, state.manga.genre)
                            },
                            // SY <--
                        )
                    }

                    // KMK -->
                    if (state.source !is StubSource &&
                        relatedMangasEnabled &&
                        state.manga.source != MERGED_SOURCE_ID
                    ) {
                        if (expandRelatedMangas) {
                            if (state.relatedMangasSorted?.isNotEmpty() != false) {
                                item { HorizontalDivider() }
                                item(
                                    key = AnimeScreenItem.RELATED_MANGAS,
                                    contentType = AnimeScreenItem.RELATED_MANGAS,
                                ) {
                                    Column {
                                        RelatedAnimeTitle(
                                            title = stringResource(KMR.strings.pref_source_related_mangas),
                                            subtitle = null,
                                            onClick = onRelatedMangasScreenClick,
                                            onLongClick = null,
                                            modifier = Modifier
                                                .padding(horizontal = MaterialTheme.padding.medium),
                                        )
                                        RelatedAnimesRow(
                                            relatedAnimes = state.relatedMangasSorted,
                                            getMangaState = getMangaState,
                                            onMangaClick = onRelatedMangaClick,
                                            onMangaLongClick = onRelatedMangaLongClick,
                                        )
                                    }
                                }
                                item { HorizontalDivider() }
                            }
                        } else if (!showRelatedMangasInOverflow) {
                            item(
                                key = AnimeScreenItem.RELATED_MANGAS,
                                contentType = AnimeScreenItem.RELATED_MANGAS,
                            ) {
                                OutlinedButtonWithArrow(
                                    text = stringResource(KMR.strings.pref_source_related_mangas)
                                        .uppercase(),
                                    onClick = onRelatedMangasScreenClick,
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

                    if (state.pagePreviewsState !is PagePreviewState.Unused && previewsRowCount > 0) {
                        PagePreviewItems(
                            pagePreviewState = state.pagePreviewsState,
                            onOpenPage = onOpenPagePreview,
                            onMorePreviewsClicked = onMorePreviewsClicked,
                            maxWidth = maxWidth,
                            setMaxWidth = { maxWidth = it },
                            rowCount = previewsRowCount,
                        )
                    }
                    // SY <--

                    item(
                        key = AnimeScreenItem.CHAPTER_HEADER,
                        contentType = AnimeScreenItem.CHAPTER_HEADER,
                    ) {
                        val missingChapterCount = remember(chapters) {
                            chapters.map { it.episode.chapterNumber }.missingEpisodesCount()
                        }
                        EpisodeHeader(
                            enabled = !isAnySelected,
                            chapterCount = chapters.size,
                            missingChapterCount = missingChapterCount,
                            onClick = onFilterClicked,
                        )
                    }

                    sharedEpisodeItems(
                        manga = state.manga,
                        mergedData = state.mergedData,
                        chapters = listItem,
                        isAnyChapterSelected = chapters.fastAny { it.selected },
                        chapterSwipeStartAction = chapterSwipeStartAction,
                        chapterSwipeEndAction = chapterSwipeEndAction,
                        // SY -->
                        alwaysShowReadingProgress = state.alwaysShowReadingProgress,
                        // SY <--
                        onChapterClicked = onChapterClicked,
                        onDownloadChapter = onDownloadChapter,
                        onChapterSelected = onChapterSelected,
                        onChapterSwipe = onChapterSwipe,
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
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    onBackClicked: () -> Unit,
    onChapterClicked: (Episode) -> Unit,
    onDownloadChapter: ((List<EpisodeList.Item>, EpisodeDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,

    // For tags menu
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,

    onFilterButtonClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueReading: () -> Unit,
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
    onMetadataViewerClicked: () -> Unit,
    onEditInfoClicked: () -> Unit,
    onRecommendClicked: () -> Unit,
    onMergedSettingsClicked: () -> Unit,
    onMergeClicked: () -> Unit,
    onMergeWithAnotherClicked: () -> Unit,
    onOpenPagePreview: (Int) -> Unit,
    onMorePreviewsClicked: () -> Unit,
    previewsRowCount: Int,
    // SY <--

    // For bottom action menu
    onMultiBookmarkClicked: (List<Episode>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<Episode>, markAsRead: Boolean) -> Unit,
    onMarkPreviousAsReadClicked: (Episode) -> Unit,
    onMultiDeleteClicked: (List<Episode>) -> Unit,

    // For swipe actions
    onChapterSwipe: (EpisodeList.Item, LibraryPreferences.ChapterSwipeAction) -> Unit,

    // Episode selection
    onChapterSelected: (EpisodeList.Item, Boolean, Boolean, Boolean) -> Unit,
    onAllChapterSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,

    // KMK -->
    getMangaState: @Composable ((Manga) -> State<Manga>),
    onRelatedMangasScreenClick: () -> Unit,
    onRelatedMangaClick: (Manga) -> Unit,
    onRelatedMangaLongClick: (Manga) -> Unit,
    librarySearch: (query: String) -> Unit,
    onSourceClick: () -> Unit,
    onCoverLoaded: (AnimeCover) -> Unit,
    coverRatio: MutableFloatState,
    onPaletteScreenClick: () -> Unit,
    hazeState: HazeState,
    // KMK <--
) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current

    val (chapters, listItem, isAnySelected) = remember(state) {
        Triple(
            first = state.processedChapters,
            second = state.episodeListItems,
            third = state.isAnySelected,
        )
    }

    // SY -->
    val metadataDescription = metadataDescription(state.source)
    // SY <--
    // KMK -->
    val uiPreferences = Injekt.get<UiPreferences>()
    val relatedMangasEnabled by Injekt.get<SourcePreferences>().relatedMangas().collectAsState()
    val expandRelatedMangas by uiPreferences.expandRelatedMangas().collectAsState()
    val showRelatedMangasInOverflow by uiPreferences.relatedMangasInOverflow().collectAsState()

    var layoutSize by remember { mutableStateOf(IntSize.Zero) }
    var fabSize by remember { mutableStateOf(IntSize.Zero) }
    var positionOnScreen by remember { mutableStateOf(Offset.Zero) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val fabPosition by uiPreferences.readButtonPosition().collectAsState()
    val readButtonPosition = uiPreferences.readButtonPosition()
    // KMK <--

    val insetPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()
    var topBarHeight by remember { mutableIntStateOf(0) }

    val chapterListState = rememberLazyListState()

    val internalOnBackPressed = {
        if (isAnySelected) {
            onAllChapterSelected(false)
        } else {
            onBackClicked()
        }
    }
    BackHandler(onBack = internalOnBackPressed)

    Scaffold(
        topBar = {
            val selectedChapterCount = remember(chapters) {
                chapters.count { it.selected }
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
                onClickRelatedMangas = onRelatedMangasScreenClick.takeIf {
                    !expandRelatedMangas &&
                        showRelatedMangasInOverflow &&
                        state.manga.source != MERGED_SOURCE_ID
                },
                // KMK <--
                onClickRecommend = onRecommendClicked.takeIf { state.showRecommendationsInOverflow },
                onClickMergedSettings = onMergedSettingsClicked.takeIf { state.manga.source == MERGED_SOURCE_ID },
                onClickMerge = onMergeClicked.takeIf { state.showMergeInOverflow },
                // SY <--
                actionModeCounter = selectedChapterCount,
                onSelectAll = { onAllChapterSelected(true) },
                onInvertSelection = { onInvertSelection() },
                // KMK -->
                onPaletteScreenClick = onPaletteScreenClick,
                // KMK <--
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomEnd,
            ) {
                val selectedChapters = remember(chapters) {
                    chapters.filter { it.selected }
                }
                SharedAnimeBottomActionMenu(
                    selected = selectedChapters,
                    onMultiBookmarkClicked = onMultiBookmarkClicked,
                    onMultiMarkAsReadClicked = onMultiMarkAsReadClicked,
                    onMarkPreviousAsReadClicked = onMarkPreviousAsReadClicked,
                    onDownloadChapter = onDownloadChapter,
                    onMultiDeleteClicked = onMultiDeleteClicked,
                    fillFraction = 0.5f,
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            val isFABVisible = remember(chapters) {
                chapters.fastAny { !it.episode.read } && !isAnySelected
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
                                    readButtonPosition.set(FabPosition.End.toString())
                                } else {
                                    readButtonPosition.set(FabPosition.Start.toString())
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
                        val isReading = remember(state.chapters) {
                            state.chapters.fastAny { it.episode.read }
                        }
                        Text(
                            text = stringResource(
                                if (isReading) MR.strings.action_resume else MR.strings.action_start,
                            ),
                        )
                    },
                    icon = { Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null) },
                    onClick = onContinueReading,
                    expanded = chapterListState.shouldExpandFAB(),
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
                            sourceName = remember { state.source.getNameForMangaInfo(state.mergedData?.sources) },
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
                        // SY -->
                        metadataDescription?.invoke(
                            state,
                            onMetadataViewerClicked,
                        ) {
                            onSearch(it, false)
                        }
                        // SY <--
                        ExpandableAnimeDescription(
                            defaultExpandState = true,
                            description = state.manga.description,
                            tagsProvider = { state.manga.genre },
                            onTagSearch = onTagSearch,
                            onCopyTagToClipboard = onCopyTagToClipboard,
                            // SY -->
                            doSearch = onSearch,
                            searchMetadataChips = remember(state.meta, state.source.id, state.manga.genre) {
                                SearchMetadataChips(state.meta, state.source, state.manga.genre)
                            },
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
                        if (state.pagePreviewsState !is PagePreviewState.Unused && previewsRowCount > 0) {
                            PagePreviews(
                                pagePreviewState = state.pagePreviewsState,
                                onOpenPage = onOpenPagePreview,
                                onMorePreviewsClicked = onMorePreviewsClicked,
                                rowCount = previewsRowCount,
                            )
                        }
                        // SY <--
                    }
                },
                endContent = {
                    VerticalFastScroller(
                        listState = chapterListState,
                        topContentPadding = contentPadding.calculateTopPadding(),
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxHeight(),
                            state = chapterListState,
                            contentPadding = PaddingValues(
                                top = contentPadding.calculateTopPadding(),
                                bottom = contentPadding.calculateBottomPadding(),
                            ),
                        ) {
                            // KMK -->
                            if (state.source !is StubSource &&
                                relatedMangasEnabled &&
                                state.manga.source != MERGED_SOURCE_ID
                            ) {
                                if (expandRelatedMangas) {
                                    if (state.relatedMangasSorted?.isNotEmpty() != false) {
                                        item(
                                            key = AnimeScreenItem.RELATED_MANGAS,
                                            contentType = AnimeScreenItem.RELATED_MANGAS,
                                        ) {
                                            Column {
                                                RelatedAnimeTitle(
                                                    title = stringResource(KMR.strings.pref_source_related_mangas)
                                                        .uppercase(),
                                                    subtitle = null,
                                                    onClick = onRelatedMangasScreenClick,
                                                    onLongClick = null,
                                                    modifier = Modifier
                                                        .padding(horizontal = MaterialTheme.padding.medium),
                                                )
                                                RelatedAnimesRow(
                                                    relatedAnimes = state.relatedMangasSorted,
                                                    getMangaState = getMangaState,
                                                    onMangaClick = onRelatedMangaClick,
                                                    onMangaLongClick = onRelatedMangaLongClick,
                                                )
                                            }
                                        }
                                        item { HorizontalDivider() }
                                    }
                                } else if (!showRelatedMangasInOverflow) {
                                    item(
                                        key = AnimeScreenItem.RELATED_MANGAS,
                                        contentType = AnimeScreenItem.RELATED_MANGAS,
                                    ) {
                                        OutlinedButtonWithArrow(
                                            text = stringResource(KMR.strings.pref_source_related_mangas),
                                            onClick = onRelatedMangasScreenClick,
                                        )
                                    }
                                }
                            }
                            // KMK <--

                            item(
                                key = AnimeScreenItem.CHAPTER_HEADER,
                                contentType = AnimeScreenItem.CHAPTER_HEADER,
                            ) {
                                val missingChapterCount = remember(chapters) {
                                    chapters.map { it.episode.chapterNumber }.missingEpisodesCount()
                                }
                                EpisodeHeader(
                                    enabled = !isAnySelected,
                                    chapterCount = chapters.size,
                                    missingChapterCount = missingChapterCount,
                                    onClick = onFilterButtonClicked,
                                )
                            }

                            sharedEpisodeItems(
                                manga = state.manga,
                                mergedData = state.mergedData,
                                chapters = listItem,
                                isAnyChapterSelected = chapters.fastAny { it.selected },
                                chapterSwipeStartAction = chapterSwipeStartAction,
                                chapterSwipeEndAction = chapterSwipeEndAction,
                                // SY -->
                                alwaysShowReadingProgress = state.alwaysShowReadingProgress,
                                // SY <--
                                onChapterClicked = onChapterClicked,
                                onDownloadChapter = onDownloadChapter,
                                onChapterSelected = onChapterSelected,
                                onChapterSwipe = onChapterSwipe,
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
    onMultiBookmarkClicked: (List<Episode>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<Episode>, markAsRead: Boolean) -> Unit,
    onMarkPreviousAsReadClicked: (Episode) -> Unit,
    onDownloadChapter: ((List<EpisodeList.Item>, EpisodeDownloadAction) -> Unit)?,
    onMultiDeleteClicked: (List<Episode>) -> Unit,
    fillFraction: Float,
    modifier: Modifier = Modifier,
) {
    AnimeBottomActionMenu(
        visible = selected.isNotEmpty(),
        modifier = modifier.fillMaxWidth(fillFraction),
        onBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected.fastMap { it.episode }, true)
        }.takeIf { selected.fastAny { !it.episode.bookmark } },
        onRemoveBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected.fastMap { it.episode }, false)
        }.takeIf { selected.fastAll { it.episode.bookmark } },
        onMarkAsReadClicked = {
            onMultiMarkAsReadClicked(selected.fastMap { it.episode }, true)
        }.takeIf { selected.fastAny { !it.episode.read } },
        onMarkAsUnreadClicked = {
            onMultiMarkAsReadClicked(selected.fastMap { it.episode }, false)
        }.takeIf { selected.fastAny { it.episode.read || it.episode.lastPageRead > 0L } },
        onMarkPreviousAsReadClicked = {
            onMarkPreviousAsReadClicked(selected[0].episode)
        }.takeIf { selected.size == 1 },
        onDownloadClicked = {
            onDownloadChapter!!(selected.toList(), EpisodeDownloadAction.START)
        }.takeIf {
            onDownloadChapter != null && selected.fastAny { it.downloadState != Download.State.DOWNLOADED }
        },
        onDeleteClicked = {
            onMultiDeleteClicked(selected.fastMap { it.episode })
        }.takeIf {
            selected.fastAny { it.downloadState == Download.State.DOWNLOADED }
        },
    )
}

private fun LazyListScope.sharedEpisodeItems(
    manga: Manga,
    mergedData: MergedMangaData?,
    chapters: List<EpisodeList>,
    isAnyChapterSelected: Boolean,
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    // SY -->
    alwaysShowReadingProgress: Boolean,
    // SY <--
    onChapterClicked: (Episode) -> Unit,
    onDownloadChapter: ((List<EpisodeList.Item>, EpisodeDownloadAction) -> Unit)?,
    onChapterSelected: (EpisodeList.Item, Boolean, Boolean, Boolean) -> Unit,
    onChapterSwipe: (EpisodeList.Item, LibraryPreferences.ChapterSwipeAction) -> Unit,
) {
    items(
        items = chapters,
        key = { item ->
            when (item) {
                // KMK: using hashcode to prevent edge-cases where the missing count might duplicate,
                // especially on merged manga
                is EpisodeList.MissingCount -> "missing-count-${item.hashCode()}"
                is EpisodeList.Item -> "episode-${item.id}"
            }
        },
        contentType = { AnimeScreenItem.CHAPTER },
    ) { item ->
        val haptic = LocalHapticFeedback.current

        when (item) {
            is EpisodeList.MissingCount -> {
                MissingEpisodeCountListItem(count = item.count)
            }
            is EpisodeList.Item -> {
                AnimeChapterListItem(
                    title = if (manga.displayMode == Manga.CHAPTER_DISPLAY_NUMBER) {
                        stringResource(
                            MR.strings.display_mode_chapter,
                            formatEpisodeNumber(item.episode.chapterNumber),
                        )
                    } else {
                        item.episode.name
                    },
                    date = item.episode.dateUpload
                        .takeIf { it > 0L }
                        ?.let {
                            // SY -->
                            if (manga.isEhBasedManga()) {
                                MetadataUtil.EX_DATE_FORMAT
                                    .format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()))
                            } else {
                                relativeDateText(item.episode.dateUpload)
                            }
                            // SY <--
                        },
                    readProgress = item.episode.lastPageRead
                        .takeIf {
                            /* SY --> */(!item.episode.read || alwaysShowReadingProgress)/* SY <-- */ && it > 0L
                        }
                        ?.let {
                            stringResource(
                                MR.strings.chapter_progress,
                                it + 1,
                            )
                        },
                    scanlator = item.episode.scanlator.takeIf {
                        !it.isNullOrBlank() /* SY --> */ && item.showScanlator /* SY <-- */
                    },
                    // SY -->
                    sourceName = item.sourceName,
                    // SY <--
                    read = item.episode.read,
                    bookmark = item.episode.bookmark,
                    selected = item.selected,
                    downloadIndicatorEnabled =
                    !isAnyChapterSelected && !(mergedData?.manga?.get(item.episode.mangaId) ?: manga).isLocal(),
                    downloadStateProvider = { item.downloadState },
                    downloadProgressProvider = { item.downloadProgress },
                    chapterSwipeStartAction = chapterSwipeStartAction,
                    chapterSwipeEndAction = chapterSwipeEndAction,
                    onLongClick = {
                        onChapterSelected(item, !item.selected, true, true)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onClick = {
                        onEpisodeItemClick(
                            chapterItem = item,
                            isAnyChapterSelected = isAnyChapterSelected,
                            onToggleSelection = { onChapterSelected(item, !item.selected, true, false) },
                            onChapterClicked = onChapterClicked,
                        )
                    },
                    onDownloadClick = if (onDownloadChapter != null) {
                        { onDownloadChapter(listOf(item), it) }
                    } else {
                        null
                    },
                    onChapterSwipe = {
                        onChapterSwipe(item, it)
                    },
                )
            }
        }
    }
}

private fun onEpisodeItemClick(
    chapterItem: EpisodeList.Item,
    isAnyChapterSelected: Boolean,
    onToggleSelection: (Boolean) -> Unit,
    onChapterClicked: (Episode) -> Unit,
) {
    when {
        chapterItem.selected -> onToggleSelection(false)
        isAnyChapterSelected -> onToggleSelection(true)
        else -> onChapterClicked(chapterItem.episode)
    }
}

// SY -->
typealias MetadataDescriptionComposable = @Composable (
    state: AnimeScreenModel.State.Success,
    openMetadataViewer: () -> Unit,
    search: (String) -> Unit,
) -> Unit

@Composable
fun metadataDescription(source: Source): MetadataDescriptionComposable? {
    val metadataSource = remember(source.id) { source.getMainSource<MetadataSource<*, *>>() }
    return remember(metadataSource) {
        when (metadataSource) {
            is EHentai -> { state, openMetadataViewer, search ->
                EHentaiDescription(state, openMetadataViewer, search)
            }
            is MangaDex -> { state, openMetadataViewer, _ ->
                MangaDexDescription(state, openMetadataViewer)
            }
            is NHentai -> { state, openMetadataViewer, _ ->
                NHentaiDescription(state, openMetadataViewer)
            }
            is EightMuses -> { state, openMetadataViewer, _ ->
                EightMusesDescription(state, openMetadataViewer)
            }
            is HBrowse -> { state, openMetadataViewer, _ ->
                HBrowseDescription(state, openMetadataViewer)
            }
            is Pururin -> { state, openMetadataViewer, _ ->
                PururinDescription(state, openMetadataViewer)
            }
            is Tsumino -> { state, openMetadataViewer, _ ->
                TsuminoDescription(state, openMetadataViewer)
            }
            else -> null
        }
    }
}
// SY <--
