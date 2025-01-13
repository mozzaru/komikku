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
        animeId: Long,
        remoteTrack: Track,
        tracker: Tracker,
    ): Int? {
        // KKM -->
        // if (tracker !is EnhancedTracker) {
        //     return
        // }
        // <-- KKM

        // Current episodes in database, sort by source's order because database's order is a mess
        val dbEpisodes = getEpisodesByAnimeId.await(animeId)
            // KMK -->
            .sortedByDescending { it.sourceOrder }
            // KMK <--
            .filter { it.isRecognizedNumber }

        val sortedEpisodes = dbEpisodes
            .sortedBy { it.episodeNumber }

        // KMK -->
        var lastCheckEpisode: Double
        var checkingEpisode = 0.0

        /**
         * Episodes to update to follow tracker: only continuous incremental episodes
         * any abnormal episode number will stop it from updating read status further.
         * Some animes has name such as Volume 2 Episode 1 which will corrupt the order
         * if we sort by episodeNumber.
         */
        val episodeUpdates = dbEpisodes
            .takeWhile { episode ->
                lastCheckEpisode = checkingEpisode
                checkingEpisode = episode.episodeNumber
                episode.episodeNumber >= lastCheckEpisode && episode.episodeNumber <= remoteTrack.lastEpisodeSeen
            }
            .filter { episode -> !episode.seen }
            // KMK <--
            .map { it.copy(seen = true).toEpisodeUpdate() }

        // only take into account continuous reading
        val localLastRead = sortedEpisodes.takeWhile { it.seen }.lastOrNull()?.episodeNumber ?: 0F
        // Tracker will update to latest read episode
        val lastRead = max(remoteTrack.lastEpisodeSeen, localLastRead.toDouble())
        val updatedTrack = remoteTrack.copy(lastEpisodeSeen = lastRead)

        try {
            // Update Tracker to localLastRead if needed
            if (lastRead > remoteTrack.lastEpisodeSeen) {
                tracker.update(updatedTrack.toDbTrack())
                // update Track in database
                insertTrack.await(updatedTrack)
            }
            // KMK -->
            // Always update local episodes following Tracker even past episodes
            if (episodeUpdates.isNotEmpty() &&
                trackPreferences.autoSyncProgressFromTrackers().get() &&
                !tracker.hasNotStartedWatching(remoteTrack.status)
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
