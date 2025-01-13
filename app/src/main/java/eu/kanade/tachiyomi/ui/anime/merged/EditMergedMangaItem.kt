package eu.kanade.tachiyomi.ui.anime.merged

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.EditMergedSettingsItemBinding
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.MergedAnimeReference

class EditMergedAnimeItem(val mergedAnime: Anime?, val mergedAnimeReference: MergedAnimeReference) : AbstractFlexibleItem<EditMergedAnimeHolder>() {

    override fun getLayoutRes(): Int {
        return R.layout.edit_merged_settings_item
    }

    override fun isDraggable(): Boolean {
        return true
    }

    lateinit var binding: EditMergedSettingsItemBinding

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): EditMergedAnimeHolder {
        binding = EditMergedSettingsItemBinding.bind(view)
        return EditMergedAnimeHolder(binding.root, adapter as EditMergedAnimeAdapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?,
        holder: EditMergedAnimeHolder,
        position: Int,
        payloads: MutableList<Any>?,
    ) {
        holder.bind(this)
    }

    override fun hashCode(): Int {
        return mergedAnimeReference.id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is EditMergedAnimeItem) {
            return mergedAnimeReference.id == other.mergedAnimeReference.id
        }
        return false
    }
}
