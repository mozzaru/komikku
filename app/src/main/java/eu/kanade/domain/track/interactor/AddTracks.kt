package eu.kanade.domain.track.interactor

import android.app.Application
import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.util.lang.convertEpochMillisZone
import eu.kanade.tachiyomi.util.system.toast
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.ZoneOffset

class AddTracks(
    private val insertTrack: InsertTrack,
    private val refreshTracks: RefreshTracks,
    private val getEpisodesByAnimeId: GetEpisodesByAnimeId,
    private val trackerManager: TrackerManager,
) {

    suspend fun bind(tracker: Tracker, item: Track, animeId: Long) = withNonCancellableContext {
        withIOContext {
            val allEpisodes = getEpisodesByAnimeId.await(animeId)
            val hasSeenEpisodes = allEpisodes.any { it.seen }
            tracker.bind(item, hasSeenEpisodes)

            var track = item.toDomainTrack(idRequired = false) ?: return@withIOContext

            insertTrack.await(track)

            // TODO: merge into [SyncEpisodeProgressWithTrack]?
            // Update episode progress if newer episodes marked seen locally
            if (hasSeenEpisodes) {
                val latestLocalSeenEpisodeNumber = allEpisodes
                    .sortedBy { it.episodeNumber }
                    .takeWhile { it.seen }
                    .lastOrNull()
                    ?.episodeNumber ?: -1.0

                if (latestLocalSeenEpisodeNumber > track.lastEpisodeSeen) {
                    track = track.copy(
                        lastEpisodeSeen = latestLocalSeenEpisodeNumber,
                    )
                    tracker.setRemoteLastEpisodeSeen(track.toDbTrack(), latestLocalSeenEpisodeNumber.toInt())
                }

                if (track.startDate <= 0) {
                    val firstSeenEpisodeDate = Injekt.get<GetHistory>().await(animeId)
                        .sortedBy { it.seenAt }
                        .firstOrNull()
                        ?.seenAt

                    firstSeenEpisodeDate?.let {
                        val startDate = firstSeenEpisodeDate.time.convertEpochMillisZone(
                            ZoneOffset.systemDefault(),
                            ZoneOffset.UTC,
                        )
                        track = track.copy(
                            startDate = startDate,
                        )
                        tracker.setRemoteStartDate(track.toDbTrack(), startDate)
                    }
                }
            }

            // KMK -->
            val context = Injekt.get<Application>()
            refreshTracks.await(animeId)
                .filter { it.first != null }
                .forEach { (track, e) ->
                    logcat(LogPriority.ERROR, e) {
                        "Failed to refresh track data animeId=$animeId for service ${track!!.id}"
                    }
                    withUIContext {
                        context.toast(
                            context.stringResource(
                                MR.strings.track_error,
                                track!!.name,
                                e.message ?: "",
                            ),
                        )
                    }
                }
            // KMK <--
        }
    }

    suspend fun bindEnhancedTrackers(anime: Anime, source: Source) = withNonCancellableContext {
        withIOContext {
            trackerManager.loggedInTrackers()
                .filterIsInstance<EnhancedTracker>()
                .filter { it.accept(source) }
                .forEach { service ->
                    try {
                        service.match(anime)?.let { track ->
                            track.anime_id = anime.id
                            (service as Tracker).bind(track)
                            insertTrack.await(track.toDomainTrack(idRequired = false)!!)
                        }
                    } catch (e: Exception) {
                        logcat(
                            LogPriority.WARN,
                            e,
                        ) { "Could not match anime: ${anime.title} with service $service" }
                    }
                }

            // KMK -->
            val context = Injekt.get<Application>()
            refreshTracks.await(anime.id)
                .filter { it.first != null }
                .forEach { (track, e) ->
                    logcat(LogPriority.ERROR, e) {
                        "Failed to refresh track data animeId=${anime.id} for service ${track!!.id}"
                    }
                    withUIContext {
                        context.toast(
                            context.stringResource(
                                MR.strings.track_error,
                                track!!.name,
                                e.message ?: "",
                            ),
                        )
                    }
                }
            // KMK <--
        }
    }
}
