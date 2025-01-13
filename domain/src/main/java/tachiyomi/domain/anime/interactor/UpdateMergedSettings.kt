package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.repository.AnimeMergeRepository

class UpdateMergedSettings(
    private val animeMergeRepository: AnimeMergeRepository,
) {

    suspend fun await(mergeUpdate: tachiyomi.domain.anime.model.MergeAnimeSettingsUpdate): Boolean {
        return animeMergeRepository.updateSettings(mergeUpdate)
    }

    suspend fun awaitAll(values: List<tachiyomi.domain.anime.model.MergeAnimeSettingsUpdate>): Boolean {
        return animeMergeRepository.updateAllSettings(values)
    }
}
