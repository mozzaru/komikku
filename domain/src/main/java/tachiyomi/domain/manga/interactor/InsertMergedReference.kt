package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.MergedAnimeReference
import tachiyomi.domain.manga.repository.AnimeMergeRepository

class InsertMergedReference(
    private val mangaMergedRepository: AnimeMergeRepository,
) {

    suspend fun await(reference: MergedAnimeReference): Long? {
        return mangaMergedRepository.insert(reference)
    }

    suspend fun awaitAll(references: List<MergedAnimeReference>) {
        mangaMergedRepository.insertAll(references)
    }
}
