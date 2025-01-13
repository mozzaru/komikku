package eu.kanade.tachiyomi.ui.anime.merged

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.databinding.EditMergedSettingsHeaderBinding
import exh.log.xLogD
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.anime.model.MergedAnimeReference
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.injectLazy

class EditMergedSettingsHeaderAdapter(private val state: EditMergedSettingsState, adapter: EditMergedAnimeAdapter) : RecyclerView.Adapter<EditMergedSettingsHeaderAdapter.HeaderViewHolder>() {

    private val sourceManager: SourceManager by injectLazy()

    private lateinit var binding: EditMergedSettingsHeaderBinding

    val editMergedAnimeItemSortingListener: SortingListener = adapter

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        binding = EditMergedSettingsHeaderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return HeaderViewHolder(binding.root)
    }

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind()
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind() {
            val dedupeAdapter: ArrayAdapter<String> = ArrayAdapter(
                itemView.context,
                android.R.layout.simple_spinner_item,
                listOfNotNull(
                    itemView.context.stringResource(SYMR.strings.no_dedupe),
                    itemView.context.stringResource(SYMR.strings.dedupe_priority),
                    itemView.context.stringResource(SYMR.strings.dedupe_most_episodes),
                    itemView.context.stringResource(SYMR.strings.dedupe_highest_episode),
                ),
            )
            dedupeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.dedupeModeSpinner.adapter = dedupeAdapter
            state.mergeReference?.let {
                binding.dedupeModeSpinner.setSelection(
                    when (it.episodeSortMode) {
                        MergedAnimeReference.CHAPTER_SORT_NO_DEDUPE -> 0
                        MergedAnimeReference.CHAPTER_SORT_PRIORITY -> 1
                        MergedAnimeReference.CHAPTER_SORT_MOST_CHAPTERS -> 2
                        MergedAnimeReference.CHAPTER_SORT_HIGHEST_CHAPTER_NUMBER -> 3
                        else -> 0
                    },
                )
            }
            binding.dedupeModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    state.mergeReference = state.mergeReference?.copy(
                        episodeSortMode = when (position) {
                            0 -> MergedAnimeReference.CHAPTER_SORT_NO_DEDUPE
                            1 -> MergedAnimeReference.CHAPTER_SORT_PRIORITY
                            2 -> MergedAnimeReference.CHAPTER_SORT_MOST_CHAPTERS
                            3 -> MergedAnimeReference.CHAPTER_SORT_HIGHEST_CHAPTER_NUMBER
                            else -> MergedAnimeReference.CHAPTER_SORT_NO_DEDUPE
                        },
                    )
                    xLogD(state.mergeReference?.episodeSortMode)
                    editMergedAnimeItemSortingListener.onSetPrioritySort(canMove())
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    state.mergeReference = state.mergeReference?.copy(
                        episodeSortMode = MergedAnimeReference.CHAPTER_SORT_NO_DEDUPE,
                    )
                }
            }

            val mergedAnimes = state.mergedAnimes

            val animeInfoAdapter: ArrayAdapter<String> = ArrayAdapter(
                itemView.context,
                android.R.layout.simple_spinner_item,
                mergedAnimes.map {
                    sourceManager.getOrStub(it.second.animeSourceId).toString() + " " + it.first?.title
                },
            )
            animeInfoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.mangaInfoSpinner.adapter = animeInfoAdapter

            mergedAnimes.indexOfFirst { it.second.isInfoAnime }.let {
                if (it != -1) {
                    binding.mangaInfoSpinner.setSelection(it)
                } else {
                    binding.mangaInfoSpinner.setSelection(0)
                }
            }

            binding.mangaInfoSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    state.mergedAnimes = state.mergedAnimes.map { (anime, reference) ->
                        anime to reference.copy(
                            isInfoAnime = reference.id == mergedAnimes.getOrNull(position)?.second?.id,
                        )
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    mergedAnimes.find { it.second.isInfoAnime }?.second?.let { newInfoAnime ->
                        state.mergedAnimes = state.mergedAnimes.map { (anime, reference) ->
                            anime to reference.copy(
                                isInfoAnime = reference.id == newInfoAnime.id,
                            )
                        }
                    }
                }
            }

            binding.dedupeSwitch.isChecked = state.mergeReference?.let {
                it.episodeSortMode != MergedAnimeReference.CHAPTER_SORT_NONE
            } ?: false
            binding.dedupeSwitch.setOnCheckedChangeListener { _, isChecked ->
                binding.dedupeModeSpinner.isEnabled = isChecked
                binding.dedupeModeSpinner.alpha = when (isChecked) {
                    true -> 1F
                    false -> 0.5F
                }
                state.mergeReference = state.mergeReference?.copy(
                    episodeSortMode = when (isChecked) {
                        true -> MergedAnimeReference.CHAPTER_SORT_NO_DEDUPE
                        false -> MergedAnimeReference.CHAPTER_SORT_NONE
                    },
                )

                if (isChecked) binding.dedupeModeSpinner.setSelection(0)
            }

            binding.dedupeModeSpinner.isEnabled = binding.dedupeSwitch.isChecked
            binding.dedupeModeSpinner.alpha = when (binding.dedupeSwitch.isChecked) {
                true -> 1F
                false -> 0.5F
            }
        }
    }

    fun canMove() =
        state.mergeReference?.let { it.episodeSortMode == MergedAnimeReference.CHAPTER_SORT_PRIORITY } ?: false

    interface SortingListener {
        fun onSetPrioritySort(isPriorityOrder: Boolean)
    }
}
