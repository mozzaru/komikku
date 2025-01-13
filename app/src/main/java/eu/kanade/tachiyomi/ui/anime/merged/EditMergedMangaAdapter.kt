package eu.kanade.tachiyomi.ui.anime.merged

import eu.davidea.flexibleadapter.FlexibleAdapter

/**
 * Adapter storing a list of merged anime.
 *
 * @param controller the context of the fragment containing this adapter.
 * @param isPriorityOrder if deduplication mode is based on priority
 */
class EditMergedAnimeAdapter(listener: EditMergedSettingsState, var isPriorityOrder: Boolean) :
    FlexibleAdapter<EditMergedAnimeItem>(null, listener, true),
    EditMergedSettingsHeaderAdapter.SortingListener {

    /**
     * Listener called when an item of the list is released.
     */
    val editMergedAnimeItemListener: EditMergedAnimeItemListener = listener

    interface EditMergedAnimeItemListener {
        fun onItemReleased(position: Int)
        fun onDeleteClick(position: Int)
        fun onToggleEpisodeUpdatesClicked(position: Int)
        fun onToggleEpisodeDownloadsClicked(position: Int)
    }

    override fun onSetPrioritySort(isPriorityOrder: Boolean) {
        isHandleDragEnabled = isPriorityOrder
        this.isPriorityOrder = isPriorityOrder
        allBoundViewHolders.onEach { editMergedAnimeHolder ->
            if (editMergedAnimeHolder is EditMergedAnimeHolder) {
                editMergedAnimeHolder.setHandelAlpha(isPriorityOrder)
            }
        }
    }
}
