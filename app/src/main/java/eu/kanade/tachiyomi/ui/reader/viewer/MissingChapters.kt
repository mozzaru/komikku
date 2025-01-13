package eu.kanade.tachiyomi.ui.reader.viewer

import eu.kanade.tachiyomi.data.database.models.toDomainEpisode
import eu.kanade.tachiyomi.ui.reader.model.ReaderEpisode
import tachiyomi.domain.episode.service.calculateEpisodeGap as domainCalculateEpisodeGap

fun calculateEpisodeGap(higherReaderEpisode: ReaderEpisode?, lowerReaderEpisode: ReaderEpisode?): Int {
    return domainCalculateEpisodeGap(
        higherReaderEpisode?.episode?.toDomainEpisode(),
        lowerReaderEpisode?.episode?.toDomainEpisode(),
    )
}
