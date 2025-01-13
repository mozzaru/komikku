package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.repository.AnimeMergeRepository

class InsertMergedReference(
    private val animeMergedRepository: AnimeMergeRepository,
) {

    suspend fun await(reference: tachiyomi.domain.anime.model.MergedAnimeReference): Long? {
        return animeMergedRepository.insert(reference)
    }

    suspend fun awaitAll(references: List<tachiyomi.domain.anime.model.MergedAnimeReference>) {
        animeMergedRepository.insertAll(references)
    }
}
