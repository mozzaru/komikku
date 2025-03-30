package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.model.MergedAnimeReference
import tachiyomi.domain.anime.repository.AnimeMergeRepository

class InsertMergedReference(
    private val animeMergedRepository: AnimeMergeRepository,
) {

    suspend fun await(reference: MergedAnimeReference): Long? {
        return animeMergedRepository.insert(reference)
    }

    suspend fun awaitAll(references: List<MergedAnimeReference>) {
        animeMergedRepository.insertAll(references)
    }
}
