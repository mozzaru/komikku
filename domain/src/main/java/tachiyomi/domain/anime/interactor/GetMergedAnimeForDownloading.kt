package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.anime.repository.AnimeMergeRepository

class GetMergedAnimeForDownloading(
    private val animeMergeRepository: AnimeMergeRepository,
) {

    suspend fun await(mergeId: Long): List<Manga> {
        return animeMergeRepository.getMergeMangaForDownloading(mergeId)
    }
}
