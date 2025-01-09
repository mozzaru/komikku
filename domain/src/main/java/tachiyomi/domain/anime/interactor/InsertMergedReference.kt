package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.model.MergedMangaReference
import tachiyomi.domain.anime.repository.MangaMergeRepository

class InsertMergedReference(
    private val mangaMergedRepository: MangaMergeRepository,
) {

    suspend fun await(reference: MergedMangaReference): Long? {
        return mangaMergedRepository.insert(reference)
    }

    suspend fun awaitAll(references: List<MergedMangaReference>) {
        mangaMergedRepository.insertAll(references)
    }
}
