package tachiyomi.domain.chapter.interactor

import tachiyomi.domain.chapter.repository.ChapterRepository

class DeleteEpisodes(
    private val chapterRepository: ChapterRepository,
) {

    suspend fun await(chapters: List<Long>) {
        chapterRepository.removeEpisodesWithIds(chapters)
    }
}
