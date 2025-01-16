package eu.kanade.domain.track.interactor

import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.track.Tracker
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.episode.interactor.UpdateEpisode
import tachiyomi.domain.episode.model.toEpisodeUpdate
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.track.model.Track
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.max

class SyncEpisodeProgressWithTrack(
    private val updateEpisode: UpdateEpisode,
    private val insertTrack: InsertTrack,
    private val getEpisodesByAnimeId: GetEpisodesByAnimeId,
) {
    val trackPreferences: TrackPreferences = Injekt.get()

    suspend fun await(
        mangaId: Long,
        remoteTrack: Track,
        tracker: Tracker,
    ): Int? {
        // KKM -->
        // if (tracker !is EnhancedTracker) {
        //     return
        // }
        // <-- KKM

        // Current chapters in database, sort by source's order because database's order is a mess
        val dbChapters = getEpisodesByAnimeId.await(mangaId)
            // KMK -->
            .sortedByDescending { it.sourceOrder }
            // KMK <--
            .filter { it.isRecognizedNumber }

        val sortedChapters = dbChapters
            .sortedBy { it.chapterNumber }

        // KMK -->
        var lastCheckChapter: Double
        var checkingChapter = 0.0

        /**
         * Chapters to update to follow tracker: only continuous incremental chapters
         * any abnormal episode number will stop it from updating read status further.
         * Some mangas has name such as Volume 2 Episode 1 which will corrupt the order
         * if we sort by chapterNumber.
         */
        val episodeUpdates = dbChapters
            .takeWhile { chapter ->
                lastCheckChapter = checkingChapter
                checkingChapter = chapter.chapterNumber
                chapter.chapterNumber >= lastCheckChapter && chapter.chapterNumber <= remoteTrack.lastChapterRead
            }
            .filter { chapter -> !chapter.read }
            // KMK <--
            .map { it.copy(read = true).toEpisodeUpdate() }

        // only take into account continuous reading
        val localLastRead = sortedChapters.takeWhile { it.read }.lastOrNull()?.chapterNumber ?: 0F
        // Tracker will update to latest read episode
        val lastRead = max(remoteTrack.lastChapterRead, localLastRead.toDouble())
        val updatedTrack = remoteTrack.copy(lastChapterRead = lastRead)

        try {
            // Update Tracker to localLastRead if needed
            if (lastRead > remoteTrack.lastChapterRead) {
                tracker.update(updatedTrack.toDbTrack())
                // update Track in database
                insertTrack.await(updatedTrack)
            }
            // KMK -->
            // Always update local chapters following Tracker even past chapters
            if (episodeUpdates.isNotEmpty() &&
                trackPreferences.autoSyncProgressFromTrackers().get() &&
                !tracker.hasNotStartedReading(remoteTrack.status)
            ) {
                updateEpisode.awaitAll(episodeUpdates)
                return lastRead.toInt()
            }
            // KMK <--
        } catch (e: Throwable) {
            logcat(LogPriority.WARN, e)
        }
        // KMK -->
        return null
        // KMK <--
    }
}
