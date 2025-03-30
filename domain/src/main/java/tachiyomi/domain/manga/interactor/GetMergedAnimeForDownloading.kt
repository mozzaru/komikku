package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.AnimeMergeRepository

class GetMergedAnimeForDownloading(
    private val animeMergeRepository: AnimeMergeRepository,
) {

    suspend fun await(mergeId: Long): List<Manga> {
        return animeMergeRepository.getMergeAnimeForDownloading(mergeId)
    }
}
