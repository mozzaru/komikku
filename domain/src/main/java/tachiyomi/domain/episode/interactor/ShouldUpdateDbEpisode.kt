package tachiyomi.domain.episode.interactor

import tachiyomi.domain.episode.model.Episode

class ShouldUpdateDbEpisode {

    fun await(dbEpisode: Episode, sourceEpisode: Episode): Boolean {
        return dbEpisode.scanlator != sourceEpisode.scanlator ||
            dbEpisode.name != sourceEpisode.name ||
            dbEpisode.dateUpload != sourceEpisode.dateUpload ||
            dbEpisode.chapterNumber != sourceEpisode.chapterNumber ||
            dbEpisode.sourceOrder != sourceEpisode.sourceOrder
    }
}
