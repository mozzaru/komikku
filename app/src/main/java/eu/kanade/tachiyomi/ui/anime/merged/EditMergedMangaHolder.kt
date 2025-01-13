package eu.kanade.tachiyomi.ui.anime.merged

import android.view.View
import coil3.load
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.EditMergedSettingsItemBinding
import eu.kanade.tachiyomi.util.system.dpToPx
import exh.ui.metadata.adapters.MetadataUIUtil.getResourceColor
import tachiyomi.domain.anime.model.MergedAnimeReference
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class EditMergedAnimeHolder(view: View, val adapter: EditMergedAnimeAdapter) : FlexibleViewHolder(view, adapter) {

    lateinit var reference: MergedAnimeReference
    var binding = EditMergedSettingsItemBinding.bind(view)

    init {
        setDragHandleView(binding.reorder)
        binding.remove.setOnClickListener {
            adapter.editMergedAnimeItemListener.onDeleteClick(bindingAdapterPosition)
        }
        binding.getChapterUpdates.setOnClickListener {
            adapter.editMergedAnimeItemListener.onToggleEpisodeUpdatesClicked(bindingAdapterPosition)
        }
        binding.download.setOnClickListener {
            adapter.editMergedAnimeItemListener.onToggleEpisodeDownloadsClicked(bindingAdapterPosition)
        }
        setHandelAlpha(adapter.isPriorityOrder)
    }

    override fun onItemReleased(position: Int) {
        super.onItemReleased(position)
        adapter.editMergedAnimeItemListener.onItemReleased(position)
    }

    fun bind(item: EditMergedAnimeItem) {
        reference = item.mergedAnimeReference
        item.mergedAnime?.let {
            binding.cover.load(it) {
                transformations(RoundedCornersTransformation(4.dpToPx.toFloat()))
            }
        }

        binding.title.text = Injekt.get<SourceManager>().getOrStub(item.mergedAnimeReference.animeSourceId).toString()
        binding.subtitle.text = item.mergedAnime?.title
        updateDownloadEpisodesIcon(item.mergedAnimeReference.downloadEpisodes)
        updateEpisodeUpdatesIcon(item.mergedAnimeReference.getEpisodeUpdates)
    }

    fun setHandelAlpha(isPriorityOrder: Boolean) {
        binding.reorder.alpha = when (isPriorityOrder) {
            true -> 1F
            false -> 0.5F
        }
    }

    fun updateDownloadEpisodesIcon(setTint: Boolean) {
        val color = if (setTint) {
            itemView.context.getResourceColor(R.attr.colorAccent)
        } else {
            itemView.context.getResourceColor(R.attr.colorOnSurface)
        }

        binding.download.drawable.setTint(color)
    }

    fun updateEpisodeUpdatesIcon(setTint: Boolean) {
        val color = if (setTint) {
            itemView.context.getResourceColor(R.attr.colorAccent)
        } else {
            itemView.context.getResourceColor(R.attr.colorOnSurface)
        }

        binding.getChapterUpdates.drawable.setTint(color)
    }
}
