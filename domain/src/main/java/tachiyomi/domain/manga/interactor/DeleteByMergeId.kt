package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.repository.AnimeMergeRepository

class DeleteByMergeId(
    private val animeMergeRepository: AnimeMergeRepository,
) {

    suspend fun await(id: Long) {
        return animeMergeRepository.deleteByMergeId(id)
    }
}
