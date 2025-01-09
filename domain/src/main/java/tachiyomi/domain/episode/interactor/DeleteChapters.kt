package tachiyomi.domain.episode.interactor

import tachiyomi.domain.episode.repository.ChapterRepository

class DeleteChapters(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(chapters: List<Long>) {
        chapterRepository.removeChaptersWithIds(chapters)
    }
}
