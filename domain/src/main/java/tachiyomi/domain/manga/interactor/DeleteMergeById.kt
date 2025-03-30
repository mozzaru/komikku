package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.repository.AnimeMergeRepository

class DeleteMergeById(
    private val animeMergeRepository: AnimeMergeRepository,
) {

    suspend fun await(id: Long) {
        return animeMergeRepository.deleteById(id)
    }
}
