package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.ui.reader.model.EpisodeTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderEpisode
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerEpisodes
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.calculateEpisodeGap
import eu.kanade.tachiyomi.util.system.createReaderThemeContext

/**
 * RecyclerView Adapter used by this [viewer] to where [ViewerEpisodes] updates are posted.
 */
class WebtoonAdapter(
    val viewer: WebtoonViewer,
    // KMK -->
    @ColorInt private val seedColor: Int? = null,
    // KMK <--
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /**
     * List of currently set items.
     */
    var items: List<Any> = emptyList()
        private set

    var currentEpisode: ReaderEpisode? = null

    /**
     * Context that has been wrapped to use the correct theme values based on the
     * current app theme and reader background color
     */
    private var readerThemedContext = viewer.activity.createReaderThemeContext()

    /**
     * Updates this adapter with the given [episodes]. It handles setting a few pages of the
     * next/previous episode to allow seamless transitions.
     */
    fun setEpisodes(episodes: ViewerEpisodes, forceTransition: Boolean) {
        val newItems = mutableListOf<Any>()

        // Forces episode transition if there is missing episodes
        val prevHasMissingEpisodes = calculateEpisodeGap(episodes.currEpisode, episodes.prevEpisode) > 0
        val nextHasMissingEpisodes = calculateEpisodeGap(episodes.nextEpisode, episodes.currEpisode) > 0

        // Add previous episode pages and transition.
        if (episodes.prevEpisode != null) {
            // We only need to add the last few pages of the previous episode, because it'll be
            // selected as the current episode when one of those pages is selected.
            val prevPages = episodes.prevEpisode.pages
            if (prevPages != null) {
                newItems.addAll(prevPages.takeLast(2))
            }
        }

        // Skip transition page if the episode is loaded & current page is not a transition page
        if (prevHasMissingEpisodes || forceTransition || episodes.prevEpisode?.state !is ReaderEpisode.State.Loaded) {
            newItems.add(EpisodeTransition.Prev(episodes.currEpisode, episodes.prevEpisode))
        }

        // Add current episode.
        val currPages = episodes.currEpisode.pages
        if (currPages != null) {
            newItems.addAll(currPages)
        }

        currentEpisode = episodes.currEpisode

        // Add next episode transition and pages.
        if (nextHasMissingEpisodes || forceTransition || episodes.nextEpisode?.state !is ReaderEpisode.State.Loaded) {
            newItems.add(EpisodeTransition.Next(episodes.currEpisode, episodes.nextEpisode))
        }

        if (episodes.nextEpisode != null) {
            // Add at most two pages, because this episode will be selected before the user can
            // swap more pages.
            val nextPages = episodes.nextEpisode.pages
            if (nextPages != null) {
                newItems.addAll(nextPages.take(2))
            }
        }

        updateItems(newItems)
    }

    private fun updateItems(newItems: List<Any>) {
        val result = DiffUtil.calculateDiff(Callback(items, newItems))
        items = newItems
        result.dispatchUpdatesTo(this)
    }

    fun refresh() {
        readerThemedContext = viewer.activity.createReaderThemeContext()
    }

    /**
     * Returns the amount of items of the adapter.
     */
    override fun getItemCount(): Int {
        return items.size
    }

    /**
     * Returns the view type for the item at the given [position].
     */
    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is ReaderPage -> PAGE_VIEW
            is EpisodeTransition -> TRANSITION_VIEW
            else -> error("Unknown view type for ${item.javaClass}")
        }
    }

    /**
     * Creates a new view holder for an item with the given [viewType].
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            PAGE_VIEW -> {
                val view = ReaderPageImageView(readerThemedContext, isWebtoon = true)
                WebtoonPageHolder(
                    view,
                    viewer,
                    // KMK -->
                    seedColor = seedColor,
                    // KMK <--
                )
            }
            TRANSITION_VIEW -> {
                val view = LinearLayout(readerThemedContext)
                WebtoonTransitionHolder(
                    view,
                    viewer,
                    // KMK -->
                    seedColor = seedColor,
                    // KMK <--
                )
            }
            else -> error("Unknown view type")
        }
    }

    /**
     * Binds an existing view [holder] with the item at the given [position].
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is WebtoonPageHolder -> holder.bind(item as ReaderPage)
            is WebtoonTransitionHolder -> holder.bind(item as EpisodeTransition)
        }
    }

    /**
     * Recycles an existing view [holder] before adding it to the view pool.
     */
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        when (holder) {
            is WebtoonPageHolder -> holder.recycle()
            is WebtoonTransitionHolder -> holder.recycle()
        }
    }

    /**
     * Diff util callback used to dispatch delta updates instead of full dataset changes.
     */
    private class Callback(
        private val oldItems: List<Any>,
        private val newItems: List<Any>,
    ) : DiffUtil.Callback() {

        /**
         * Returns true if these two items are the same.
         */
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]

            return oldItem == newItem
        }

        /**
         * Returns true if the contents of the items are the same.
         */
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return true
        }

        /**
         * Returns the size of the old list.
         */
        override fun getOldListSize(): Int {
            return oldItems.size
        }

        /**
         * Returns the size of the new list.
         */
        override fun getNewListSize(): Int {
            return newItems.size
        }
    }
}

/**
 * View holder type of a episode page view.
 */
private const val PAGE_VIEW = 0

/**
 * View holder type of a episode transition view.
 */
private const val TRANSITION_VIEW = 1
