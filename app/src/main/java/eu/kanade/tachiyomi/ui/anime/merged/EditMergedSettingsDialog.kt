package eu.kanade.tachiyomi.ui.anime.merged

import android.content.Context
import android.view.LayoutInflater
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.databinding.EditMergedSettingsDialogBinding
import eu.kanade.tachiyomi.ui.anime.MergedAnimeData
import eu.kanade.tachiyomi.util.system.toast
import exh.source.MERGED_SOURCE_ID
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.MergedAnimeReference
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource

@Stable
class EditMergedSettingsState(
    private val context: Context,
    private val onDeleteClick: (MergedAnimeReference) -> Unit,
    private val onDismissRequest: () -> Unit,
    private val onPositiveClick: (List<MergedAnimeReference>) -> Unit,
) : EditMergedAnimeAdapter.EditMergedAnimeItemListener {
    var mergedAnimes: List<Pair<Anime?, MergedAnimeReference>> by mutableStateOf(emptyList())
    var mergeReference: MergedAnimeReference? by mutableStateOf(null)
    var mergedAnimeAdapter: EditMergedAnimeAdapter? by mutableStateOf(null)
    var mergedAnimeHeaderAdapter: EditMergedSettingsHeaderAdapter? by mutableStateOf(null)

    fun onViewCreated(
        context: Context,
        binding: EditMergedSettingsDialogBinding,
        mergedAnime: List<Anime>,
        mergedReferences: List<MergedAnimeReference>,
    ) {
        if (mergedReferences.isEmpty() || mergedReferences.size == 1) {
            context.toast(SYMR.strings.merged_references_invalid)
            onDismissRequest()
        }
        mergedAnimes += mergedReferences.filter {
            it.animeSourceId != MERGED_SOURCE_ID
        }.map { reference -> mergedAnime.firstOrNull { it.id == reference.animeId } to reference }
        mergeReference = mergedReferences.firstOrNull { it.animeSourceId == MERGED_SOURCE_ID }

        val isPriorityOrder =
            mergeReference?.let { it.episodeSortMode == MergedAnimeReference.CHAPTER_SORT_PRIORITY } ?: false

        mergedAnimeAdapter = EditMergedAnimeAdapter(this, isPriorityOrder)
        mergedAnimeHeaderAdapter = EditMergedSettingsHeaderAdapter(this, mergedAnimeAdapter!!)

        binding.recycler.adapter = ConcatAdapter(mergedAnimeHeaderAdapter, mergedAnimeAdapter)
        binding.recycler.layoutManager = LinearLayoutManager(context)

        mergedAnimeAdapter?.isHandleDragEnabled = isPriorityOrder

        mergedAnimeAdapter?.updateDataSet(
            mergedAnimes.map {
                it.toModel()
            }.sortedBy { it.mergedAnimeReference.episodePriority },
        )
    }

    override fun onItemReleased(position: Int) {
        val mergedAnimeAdapter = mergedAnimeAdapter ?: return
        mergedAnimes = mergedAnimes.map { (anime, reference) ->
            anime to reference.copy(
                episodePriority = mergedAnimeAdapter.currentItems.indexOfFirst {
                    reference.id == it.mergedAnimeReference.id
                },
            )
        }
    }

    override fun onDeleteClick(position: Int) {
        val mergedAnimeAdapter = mergedAnimeAdapter ?: return
        val mergeAnimeReference = mergedAnimeAdapter.currentItems.getOrNull(position)?.mergedAnimeReference ?: return

        MaterialAlertDialogBuilder(context)
            .setTitle(SYMR.strings.delete_merged_entry.getString(context))
            .setMessage(SYMR.strings.delete_merged_entry_desc.getString(context))
            .setPositiveButton(MR.strings.action_ok.getString(context)) { _, _ ->
                onDeleteClick(mergeAnimeReference)
                onDismissRequest()
            }
            .setNegativeButton(MR.strings.action_cancel.getString(context), null)
            .show()
    }

    override fun onToggleEpisodeUpdatesClicked(position: Int) {
        MaterialAlertDialogBuilder(context)
            .setTitle(SYMR.strings.episode_updates_merged_entry.getString(context))
            .setMessage(SYMR.strings.episode_updates_merged_entry_desc.getString(context))
            .setPositiveButton(MR.strings.action_ok.getString(context)) { _, _ ->
                toggleEpisodeUpdates(position)
            }
            .setNegativeButton(MR.strings.action_cancel.getString(context), null)
            .show()
    }

    private fun toggleEpisodeUpdates(position: Int) {
        val adapterReference = mergedAnimeAdapter?.currentItems?.getOrNull(position)?.mergedAnimeReference
            ?: return
        mergedAnimes = mergedAnimes.map { pair ->
            val (anime, reference) = pair
            if (reference.id != adapterReference.id) return@map pair

            mergedAnimeAdapter?.allBoundViewHolders?.firstOrNull {
                it is EditMergedAnimeHolder && it.reference.id == reference.id
            }?.let {
                if (it is EditMergedAnimeHolder) {
                    it.updateEpisodeUpdatesIcon(!reference.getEpisodeUpdates)
                }
            } ?: context.toast(SYMR.strings.merged_episode_updates_error)

            anime to reference.copy(getEpisodeUpdates = !reference.getEpisodeUpdates)
        }
    }

    override fun onToggleEpisodeDownloadsClicked(position: Int) {
        MaterialAlertDialogBuilder(context)
            .setTitle(SYMR.strings.download_merged_entry.getString(context))
            .setMessage(SYMR.strings.download_merged_entry_desc.getString(context))
            .setPositiveButton(MR.strings.action_ok.getString(context)) { _, _ ->
                toggleEpisodeDownloads(position)
            }
            .setNegativeButton(MR.strings.action_cancel.getString(context), null)
            .show()
    }

    private fun toggleEpisodeDownloads(position: Int) {
        val adapterReference = mergedAnimeAdapter?.currentItems?.getOrNull(position)?.mergedAnimeReference
            ?: return
        mergedAnimes = mergedAnimes.map { pair ->
            val (anime, reference) = pair
            if (reference.id != adapterReference.id) return@map pair

            mergedAnimeAdapter?.allBoundViewHolders?.firstOrNull {
                it is EditMergedAnimeHolder && it.reference.id == reference.id
            }?.let {
                if (it is EditMergedAnimeHolder) {
                    it.updateDownloadEpisodesIcon(!reference.downloadEpisodes)
                }
            } ?: context.toast(SYMR.strings.merged_toggle_download_episodes_error)

            anime to reference.copy(downloadEpisodes = !reference.downloadEpisodes)
        }
    }

    fun onPositiveButtonClick() {
        onPositiveClick(listOfNotNull(mergeReference) + mergedAnimes.map { it.second })
        onDismissRequest()
    }
}

@Composable
fun EditMergedSettingsDialog(
    onDismissRequest: () -> Unit,
    mergedData: MergedAnimeData,
    onDeleteClick: (MergedAnimeReference) -> Unit,
    onPositiveClick: (List<MergedAnimeReference>) -> Unit,
) {
    val context = LocalContext.current
    val state = remember {
        EditMergedSettingsState(context, onDeleteClick, onDismissRequest, onPositiveClick)
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = state::onPositiveButtonClick) {
                Text(stringResource(MR.strings.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                AndroidView(
                    factory = { factoryContext ->
                        val binding = EditMergedSettingsDialogBinding.inflate(LayoutInflater.from(factoryContext))
                        state.onViewCreated(factoryContext, binding, mergedData.anime.values.toList(), mergedData.references)
                        binding.root
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
        ),
    )
}

private fun Pair<Anime?, MergedAnimeReference>.toModel(): EditMergedAnimeItem {
    return EditMergedAnimeItem(first, second)
}
