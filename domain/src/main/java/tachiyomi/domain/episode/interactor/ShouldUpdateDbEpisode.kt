package tachiyomi.domain.episode.interactor

import tachiyomi.domain.episode.model.Chapter

class ShouldUpdateDbEpisode {

    fun await(dbChapter: Chapter, sourceChapter: Chapter): Boolean {
        return dbChapter.scanlator != sourceChapter.scanlator ||
            dbChapter.name != sourceChapter.name ||
            dbChapter.dateUpload != sourceChapter.dateUpload ||
            dbChapter.chapterNumber != sourceChapter.chapterNumber ||
            dbChapter.sourceOrder != sourceChapter.sourceOrder
    }
}
