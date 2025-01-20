package eu.kanade.tachiyomi.ui.browse

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.presentation.anime.DuplicateAnimeDialog
import eu.kanade.presentation.anime.DuplicateAnimesDialog
import eu.kanade.presentation.browse.components.RemoveAnimeDialog
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.BulkSelectionToolbar
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.ui.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.browse.migration.advanced.design.PreMigrationScreen
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.util.removeCovers
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.mapAsCheckboxState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.UnsortedPreferences
import tachiyomi.domain.anime.interactor.GetDuplicateLibraryAnime
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.toAnimeUpdate
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetAnimeCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.episode.interactor.SetAnimeDefaultEpisodeFlags
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant

class BulkFavoriteScreenModel(
    initialState: State = State(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val getDuplicateLibraryAnime: GetDuplicateLibraryAnime = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val setAnimeCategories: SetAnimeCategories = Injekt.get(),
    private val updateAnime: UpdateAnime = Injekt.get(),
    private val coverCache: CoverCache = Injekt.get(),
    private val setAnimeDefaultEpisodeFlags: SetAnimeDefaultEpisodeFlags = Injekt.get(),
    private val addTracks: AddTracks = Injekt.get(),
) : StateScreenModel<BulkFavoriteScreenModel.State>(initialState) {

    fun backHandler() {
        toggleSelectionMode()
    }

    fun toggleSelectionMode() {
        if (state.value.selectionMode) {
            clearSelection()
        }
        mutableState.update { it.copy(selectionMode = !it.selectionMode) }
    }

    private fun clearSelection() {
        mutableState.update { it.copy(selection = persistentListOf()) }
    }

    fun select(anime: Anime) {
        toggleSelection(anime, toSelectedState = true)
    }

    /**
     * @param toSelectedState set to true to only Select, set to false to only Unselect
     */
    fun toggleSelection(anime: Anime, toSelectedState: Boolean? = null) {
        mutableState.update { state ->
            val newSelection = state.selection.mutate { list ->
                if (toSelectedState != true && list.fastAny { it.id == anime.id }) {
                    list.removeAll { it.id == anime.id }
                } else if (toSelectedState != false && list.none { it.id == anime.id }) {
                    list.add(anime)
                }
            }
            state.copy(
                selection = newSelection,
                selectionMode = newSelection.isNotEmpty(),
            )
        }
    }

    fun reverseSelection(animes: List<Anime>) {
        mutableState.update { state ->
            val newSelection = animes.filterNot { manga ->
                state.selection.contains(manga)
            }
                .fastDistinctBy { it.id }
                .toPersistentList()
            state.copy(
                selection = newSelection,
                selectionMode = newSelection.isNotEmpty(),
            )
        }
    }

    /**
     * Called when user click on [BulkSelectionToolbar]'s `Favorite` button.
     * It will then look for any duplicated animes.
     * - If there is any, it will show the [DuplicateAnimesDialog].
     * - If not then it will call the [addFavoriteDuplicate].
     */
    fun addFavorite(startIdx: Int = 0) {
        screenModelScope.launch {
            startRunning()
            val mangaWithDup = getDuplicateLibraryManga(startIdx)
            if (mangaWithDup != null) {
                setDialog(Dialog.AllowDuplicate(mangaWithDup))
            } else {
                addFavoriteDuplicate()
            }
        }
    }

    /**
     * Add animes to library if there is default category or no category exists.
     * If not, it shows the categories list.
     */
    fun addFavoriteDuplicate(skipAllDuplicates: Boolean = false) {
        screenModelScope.launch {
            val mangaList = if (skipAllDuplicates) getNotDuplicateLibraryMangas() else state.value.selection
            if (mangaList.isEmpty()) {
                stopRunning()
                toggleSelectionMode()
                return@launch
            }
            val categories = getCategories()
            val defaultCategoryId = libraryPreferences.defaultCategory().get()
            val defaultCategory = categories.find { it.id == defaultCategoryId.toLong() }

            when {
                // Default category set
                defaultCategory != null -> {
                    stopRunning()
                    setMangasCategories(mangaList, listOf(defaultCategory.id), emptyList())
                }

                // Automatic 'Default' or no categories
                defaultCategoryId == 0 || categories.isEmpty() -> {
                    stopRunning()
                    // Automatic 'Default' or no categories
                    setMangasCategories(mangaList, emptyList(), emptyList())
                }

                else -> {
                    // Get indexes of the common categories to preselect.
                    val common = getCommonCategories(mangaList)
                    // Get indexes of the mix categories to preselect.
                    val mix = getMixCategories(mangaList)
                    val preselected = categories
                        .map {
                            when (it) {
                                in common -> CheckboxState.State.Checked(it)
                                in mix -> CheckboxState.TriState.Exclude(it)
                                else -> CheckboxState.State.None(it)
                            }
                        }
                        .toImmutableList()
                    stopRunning()
                    setDialog(Dialog.ChangeMangasCategory(mangaList, preselected))
                }
            }
        }
    }

    private suspend fun getNotDuplicateLibraryMangas(): List<Anime> {
        return state.value.selection.filterNot { manga ->
            getDuplicateLibraryAnime.await(manga).isNotEmpty()
        }
    }

    private suspend fun getDuplicateLibraryManga(startIdx: Int = 0): Pair<Int, Anime>? {
        val mangas = state.value.selection
        mangas.fastForEachIndexed { index, manga ->
            if (index < startIdx) return@fastForEachIndexed
            val dup = getDuplicateLibraryAnime.await(manga)
            if (dup.isEmpty()) return@fastForEachIndexed
            return Pair(index, dup.first())
        }
        return null
    }

    fun removeDuplicateSelectedManga(index: Int) {
        mutableState.update { state ->
            val newSelection = state.selection.mutate { list ->
                list.removeAt(index)
            }
            state.copy(selection = newSelection)
        }
    }

    /**
     * Bulk update categories of anime using old and new common categories.
     *
     * @param animeList the list of anime to move.
     * @param addCategories the categories to add for all animes.
     * @param removeCategories the categories to remove in all animes.
     */
    fun setMangasCategories(animeList: List<Anime>, addCategories: List<Long>, removeCategories: List<Long>) {
        screenModelScope.launchNonCancellable {
            startRunning()
            animeList.fastForEach { manga ->
                val categoryIds = getCategories.await(manga.id)
                    .map { it.id }
                    .subtract(removeCategories.toSet())
                    .plus(addCategories)
                    .toList()

                moveMangaToCategoriesAndAddToLibrary(manga, categoryIds)
            }
            stopRunning()
        }
        toggleSelectionMode()
    }

    private fun moveMangaToCategoriesAndAddToLibrary(anime: Anime, categories: List<Long>) {
        moveMangaToCategory(anime.id, categories)
        if (anime.favorite) return

        screenModelScope.launchIO {
            updateAnime.awaitUpdateFavorite(anime.id, true)
        }
    }

    private fun moveMangaToCategory(mangaId: Long, categoryIds: List<Long>) {
        screenModelScope.launchIO {
            setAnimeCategories.await(mangaId, categoryIds)
        }
    }

    /**
     * Returns the common categories for the given list of anime.
     *
     * @param animes the list of anime.
     */
    private suspend fun getCommonCategories(animes: List<Anime>): Collection<Category> {
        if (animes.isEmpty()) return emptyList()
        return animes
            .map { getCategories.await(it.id).toSet() }
            .reduce { set1, set2 -> set1.intersect(set2) }
    }

    /**
     * Returns the mix (non-common) categories for the given list of anime.
     *
     * @param animes the list of anime.
     */
    private suspend fun getMixCategories(animes: List<Anime>): Collection<Category> {
        if (animes.isEmpty()) return emptyList()
        val mangaCategories = animes.map { getCategories.await(it.id).toSet() }
        val common = mangaCategories.reduce { set1, set2 -> set1.intersect(set2) }
        return mangaCategories.flatten().distinct().subtract(common)
    }

    /**
     * Get user categories.
     *
     * @return List of categories, not including the default category
     */
    suspend fun getCategories(): List<Category> {
        return getCategories.subscribe()
            .firstOrNull()
            ?.filterNot { it.isSystemCategory }
            .orEmpty()
    }

    private suspend fun getDuplicateLibraryManga(anime: Anime): Anime? {
        return getDuplicateLibraryAnime.await(anime).getOrNull(0)
    }

    private fun moveMangaToCategories(anime: Anime, vararg categories: Category) {
        moveMangaToCategories(anime, categories.filter { it.id != 0L }.map { it.id })
    }

    fun moveMangaToCategories(anime: Anime, categoryIds: List<Long>) {
        screenModelScope.launchIO {
            setAnimeCategories.await(
                mangaId = anime.id,
                categoryIds = categoryIds.toList(),
            )
        }
    }

    /**
     * Adds or removes a anime from the library.
     *
     * @param anime the anime to update.
     */
    fun changeMangaFavorite(anime: Anime) {
        val source = sourceManager.getOrStub(anime.source)

        screenModelScope.launch {
            var new = anime.copy(
                favorite = !anime.favorite,
                dateAdded = when (anime.favorite) {
                    true -> 0
                    false -> Instant.now().toEpochMilli()
                },
            )
            // TODO: also allow deleting episodes when remove favorite (just like in [AnimeScreenModel])
            if (!new.favorite) {
                new = new.removeCovers(coverCache)
            } else {
                setAnimeDefaultEpisodeFlags.await(anime)
                addTracks.bindEnhancedTrackers(anime, source)
            }

            updateAnime.await(new.toAnimeUpdate())
        }
    }

    fun addFavorite(anime: Anime) {
        screenModelScope.launch {
            val categories = getCategories()
            val defaultCategoryId = libraryPreferences.defaultCategory().get()
            val defaultCategory = categories.find { it.id == defaultCategoryId.toLong() }

            when {
                // Default category set
                defaultCategory != null -> {
                    moveMangaToCategories(anime, defaultCategory)
                    changeMangaFavorite(anime)
                }

                // Automatic 'Default' or no categories
                defaultCategoryId == 0 || categories.isEmpty() -> {
                    moveMangaToCategories(anime)
                    changeMangaFavorite(anime)
                }

                // Choose a category
                else -> {
                    val preselectedIds = getCategories.await(anime.id).map { it.id }
                    setDialog(
                        Dialog.ChangeMangaCategory(
                            anime,
                            categories.mapAsCheckboxState { it.id in preselectedIds }.toImmutableList(),
                        ),
                    )
                }
            }
        }
    }

    fun addRemoveManga(anime: Anime, haptic: HapticFeedback? = null) {
        screenModelScope.launchIO {
            val duplicateManga = getDuplicateLibraryManga(anime)
            when {
                anime.favorite -> setDialog(
                    Dialog.RemoveManga(anime),
                )
                duplicateManga != null -> setDialog(
                    Dialog.AddDuplicateManga(
                        anime,
                        duplicateManga,
                    ),
                )
                else -> addFavorite(anime)
            }
            haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    private fun setDialog(dialog: Dialog?) {
        mutableState.update {
            it.copy(dialog = dialog)
        }
    }

    fun dismissDialog() {
        mutableState.update {
            it.copy(dialog = null)
        }
    }

    private fun startRunning() {
        mutableState.update {
            it.copy(isRunning = true)
        }
    }

    fun stopRunning() {
        mutableState.update {
            it.copy(isRunning = false)
        }
    }

    interface Dialog {
        data class RemoveManga(val anime: Anime) : Dialog
        data class AddDuplicateManga(val anime: Anime, val duplicate: Anime) : Dialog
        data class ChangeMangaCategory(
            val anime: Anime,
            val initialSelection: ImmutableList<CheckboxState.State<Category>>,
        ) : Dialog
        data class ChangeMangasCategory(
            val animes: List<Anime>,
            val initialSelection: ImmutableList<CheckboxState<Category>>,
        ) : Dialog
        data class AllowDuplicate(val duplicatedAnime: Pair<Int, Anime>) : Dialog
    }

    @Immutable
    data class State(
        val dialog: Dialog? = null,
        val selection: PersistentList<Anime> = persistentListOf(),
        val selectionMode: Boolean = false,
        val isRunning: Boolean = false,
    )
}

@Composable
fun AddDuplicateAnimeDialog(bulkFavoriteScreenModel: BulkFavoriteScreenModel) {
    val navigator = LocalNavigator.currentOrThrow
    val bulkFavoriteState by bulkFavoriteScreenModel.state.collectAsState()
    val dialog = bulkFavoriteState.dialog as BulkFavoriteScreenModel.Dialog.AddDuplicateManga

    DuplicateAnimeDialog(
        onDismissRequest = bulkFavoriteScreenModel::dismissDialog,
        onConfirm = { bulkFavoriteScreenModel.addFavorite(dialog.anime) },
        onOpenManga = { navigator.push(AnimeScreen(dialog.duplicate.id)) },
        onMigrate = {
            PreMigrationScreen.navigateToMigration(
                Injekt.get<UnsortedPreferences>().skipPreMigration().get(),
                navigator,
                dialog.duplicate.id,
                dialog.anime.id,
            )
        },
        duplicate = dialog.duplicate,
    )
}

@Composable
fun RemoveAnimeDialog(bulkFavoriteScreenModel: BulkFavoriteScreenModel) {
    val bulkFavoriteState by bulkFavoriteScreenModel.state.collectAsState()
    val dialog = bulkFavoriteState.dialog as BulkFavoriteScreenModel.Dialog.RemoveManga

    RemoveAnimeDialog(
        onDismissRequest = bulkFavoriteScreenModel::dismissDialog,
        onConfirm = {
            bulkFavoriteScreenModel.changeMangaFavorite(dialog.anime)
        },
        animeToRemove = dialog.anime,
    )
}

@Composable
fun ChangeAnimeCategoryDialog(bulkFavoriteScreenModel: BulkFavoriteScreenModel) {
    val navigator = LocalNavigator.currentOrThrow
    val bulkFavoriteState by bulkFavoriteScreenModel.state.collectAsState()
    val dialog = bulkFavoriteState.dialog as BulkFavoriteScreenModel.Dialog.ChangeMangaCategory

    ChangeCategoryDialog(
        initialSelection = dialog.initialSelection,
        onDismissRequest = bulkFavoriteScreenModel::dismissDialog,
        onEditCategories = { navigator.push(CategoryScreen()) },
        onConfirm = { include, _ ->
            bulkFavoriteScreenModel.changeMangaFavorite(dialog.anime)
            bulkFavoriteScreenModel.moveMangaToCategories(dialog.anime, include)
        },
    )
}

@Composable
fun ChangeAnimesCategoryDialog(bulkFavoriteScreenModel: BulkFavoriteScreenModel) {
    val navigator = LocalNavigator.currentOrThrow
    val bulkFavoriteState by bulkFavoriteScreenModel.state.collectAsState()
    val dialog = bulkFavoriteState.dialog as BulkFavoriteScreenModel.Dialog.ChangeMangasCategory

    ChangeCategoryDialog(
        initialSelection = dialog.initialSelection,
        onDismissRequest = bulkFavoriteScreenModel::dismissDialog,
        onEditCategories = { navigator.push(CategoryScreen()) },
        onConfirm = { include, exclude ->
            bulkFavoriteScreenModel.setMangasCategories(dialog.animes, include, exclude)
        },
    )
}

@Composable
fun AllowDuplicateDialog(bulkFavoriteScreenModel: BulkFavoriteScreenModel) {
    val navigator = LocalNavigator.currentOrThrow
    val bulkFavoriteState by bulkFavoriteScreenModel.state.collectAsState()
    val dialog = bulkFavoriteState.dialog as BulkFavoriteScreenModel.Dialog.AllowDuplicate

    DuplicateAnimesDialog(
        onDismissRequest = bulkFavoriteScreenModel::dismissDialog,
        onAllowAllDuplicate = bulkFavoriteScreenModel::addFavoriteDuplicate,
        onSkipAllDuplicate = {
            bulkFavoriteScreenModel.addFavoriteDuplicate(skipAllDuplicates = true)
        },
        onOpenManga = {
            navigator.push(AnimeScreen(dialog.duplicatedAnime.second.id))
        },
        onAllowDuplicate = {
            bulkFavoriteScreenModel.addFavorite(startIdx = dialog.duplicatedAnime.first + 1)
        },
        onSkipDuplicate = {
            bulkFavoriteScreenModel.removeDuplicateSelectedManga(index = dialog.duplicatedAnime.first)
            bulkFavoriteScreenModel.addFavorite(startIdx = dialog.duplicatedAnime.first)
        },
        mangaName = dialog.duplicatedAnime.second.title,
        stopRunning = bulkFavoriteScreenModel::stopRunning,
        duplicate = dialog.duplicatedAnime.second,
    )
}

@Composable
fun bulkSelectionButton(
    isRunning: Boolean,
    toggleSelectionMode: () -> Unit,
) = AppBar.Action(
    title = stringResource(KMR.strings.action_bulk_select),
    icon = Icons.Outlined.Checklist,
    iconTint = MaterialTheme.colorScheme.primary.takeIf { isRunning },
    onClick = toggleSelectionMode,
)
