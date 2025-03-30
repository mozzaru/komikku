package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.MergeAnimeSettingsUpdate
import tachiyomi.domain.manga.repository.AnimeMergeRepository

class UpdateMergedSettings(
    private val animeMergeRepository: AnimeMergeRepository,
) {

    suspend fun await(mergeUpdate: MergeAnimeSettingsUpdate): Boolean {
        return animeMergeRepository.updateSettings(mergeUpdate)
    }

    suspend fun awaitAll(values: List<MergeAnimeSettingsUpdate>): Boolean {
        return animeMergeRepository.updateAllSettings(values)
    }
}
