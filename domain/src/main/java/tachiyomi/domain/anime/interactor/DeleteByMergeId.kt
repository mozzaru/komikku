package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.repository.MangaMergeRepository

class DeleteByMergeId(
    private val mangaMergeRepository: MangaMergeRepository,
) {

    suspend fun await(id: Long) {
        return mangaMergeRepository.deleteByMergeId(id)
    }
}
