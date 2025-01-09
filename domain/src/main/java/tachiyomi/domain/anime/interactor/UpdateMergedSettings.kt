package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.model.MergeMangaSettingsUpdate
import tachiyomi.domain.anime.repository.MangaMergeRepository

class UpdateMergedSettings(
    private val mangaMergeRepository: MangaMergeRepository,
) {

    suspend fun await(mergeUpdate: MergeMangaSettingsUpdate): Boolean {
        return mangaMergeRepository.updateSettings(mergeUpdate)
    }

    suspend fun awaitAll(values: List<MergeMangaSettingsUpdate>): Boolean {
        return mangaMergeRepository.updateAllSettings(values)
    }
}
