package eu.kanade.tachiyomi.data.track.mdlist

import android.graphics.Color
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.model.TrackAnimeMetadata
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SAnime
import exh.md.network.MangaDexAuthInterceptor
import exh.md.utils.FollowStatus
import exh.md.utils.MdUtil
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.anime.model.Anime
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.domain.track.model.Track as DomainTrack

class MdList(id: Long) : BaseTracker(id, "MDList") {

    companion object {
        private val SCORE_LIST = IntRange(0, 10)
            .map(Int::toString)
            .toImmutableList()
    }

    private val mdex by lazy { MdUtil.getEnabledMangaDex(Injekt.get()) }

    val interceptor = MangaDexAuthInterceptor(trackPreferences, this)

    override fun getLogo(): Int {
        return R.drawable.ic_tracker_mangadex_logo
    }

    override fun getLogoColor(): Int {
        return Color.rgb(43, 48, 53)
    }

    override fun getStatusList(): List<Long> {
        return FollowStatus.entries.map { it.long }
    }

    override fun getStatus(status: Long): StringResource? = when (status) {
        0L -> SYMR.strings.md_follows_unfollowed
        1L -> MR.strings.reading
        2L -> MR.strings.completed
        3L -> MR.strings.on_hold
        4L -> MR.strings.plan_to_read
        5L -> MR.strings.dropped
        6L -> MR.strings.repeating
        else -> null
    }

    override fun getScoreList() = SCORE_LIST

    override fun displayScore(track: DomainTrack) = track.score.toInt().toString()

    override suspend fun update(track: Track, didSeenEpisode: Boolean): Track {
        return withIOContext {
            val mdex = mdex ?: throw MangaDexNotFoundException()

            val remoteTrack = mdex.fetchTrackingInfo(track.tracking_url)
            val followStatus = FollowStatus.fromLong(track.status)

            // this updates the follow status in the metadata
            // allow follow status to update
            if (remoteTrack.status != followStatus.long) {
                if (mdex.updateFollowStatus(MdUtil.getAnimeId(track.tracking_url), followStatus)) {
                    remoteTrack.status = followStatus.long
                } else {
                    track.status = remoteTrack.status
                }
            }

            if (remoteTrack.score != track.score) {
                mdex.updateRating(track)
            }

            // mangadex wont update episodes if anime is not follows this prevents unneeded network call

            /*if (followStatus != FollowStatus.UNFOLLOWED) {
                if (track.total_episodes != 0 && track.last_episode_read == track.total_episodes) {
                    track.status = FollowStatus.COMPLETED.int
                    mdex.updateFollowStatus(MdUtil.getAnimeId(track.tracking_url), FollowStatus.COMPLETED)
                }
                if (followStatus == FollowStatus.PLAN_TO_READ && track.last_episode_read > 0) {
                    val newFollowStatus = FollowStatus.READING
                    track.status = FollowStatus.READING.int
                    mdex.updateFollowStatus(MdUtil.getAnimeId(track.tracking_url), newFollowStatus)
                    remoteTrack.status = newFollowStatus.int
                }

                mdex.updateReadingProgress(track)
            } else if (track.last_episode_read != 0) {
                // When followStatus has been changed to unfollowed 0 out read episodes since dex does
                track.last_episode_read = 0
            }*/
            track
        }
    }

    override fun getCompletionStatus(): Long = FollowStatus.COMPLETED.long

    override fun getReadingStatus(): Long = FollowStatus.READING.long

    override fun getRereadingStatus(): Long = FollowStatus.RE_READING.long

    override suspend fun bind(track: Track, hasSeenEpisodes: Boolean): Track = update(
        refresh(track).also {
            if (it.status == FollowStatus.UNFOLLOWED.long) {
                it.status = if (hasSeenEpisodes) {
                    FollowStatus.READING.long
                } else {
                    FollowStatus.PLAN_TO_READ.long
                }
            }
        },
    )

    override suspend fun refresh(track: Track): Track {
        return withIOContext {
            val mdex = mdex ?: throw MangaDexNotFoundException()
            val remoteTrack = mdex.fetchTrackingInfo(track.tracking_url)
            track.copyPersonalFrom(remoteTrack)
            /*if (track.total_episodes == 0 && animeMetadata.status == SAnime.COMPLETED) {
                track.total_episodes = animeMetadata.maxEpisodeNumber ?: 0
            }*/
            track
        }
    }

    fun createInitialTracker(dbAnime: Anime, mdAnime: Anime = dbAnime): Track {
        return Track.create(id).apply {
            anime_id = dbAnime.id
            status = FollowStatus.UNFOLLOWED.long
            tracking_url = MdUtil.baseUrl + mdAnime.url
            title = mdAnime.title
        }
    }

    override suspend fun search(query: String): List<TrackSearch> {
        return withIOContext {
            val mdex = mdex ?: throw MangaDexNotFoundException()
            mdex.getSearchAnime(1, query, FilterList())
                .animes
                .map {
                    toTrackSearch(mdex.getAnimeDetails(it))
                }
                .distinct()
        }
    }

    private fun toTrackSearch(animeInfo: SAnime): TrackSearch = TrackSearch.create(id).apply {
        tracking_url = MdUtil.baseUrl + animeInfo.url
        title = animeInfo.title
        cover_url = animeInfo.thumbnail_url.orEmpty()
        summary = animeInfo.description.orEmpty()
    }

    override suspend fun login(username: String, password: String): Unit = throw Exception("not used")

    override fun logout() {
        super.logout()
        trackPreferences.trackToken(this).delete()
    }

    override suspend fun getAnimeMetadata(track: DomainTrack): TrackAnimeMetadata? {
        return withIOContext {
            val mdex = mdex ?: throw MangaDexNotFoundException()
            val anime = mdex.getAnimeMetadata(track.toDbTrack())
            TrackAnimeMetadata(
                remoteId = 0,
                title = anime?.title,
                thumbnailUrl = anime?.thumbnail_url, // Doesn't load the actual cover because of Refer header
                description = anime?.description,
                authors = anime?.author,
                artists = anime?.artist,
            )
        }
    }

    override val isLoggedIn: Boolean
        get() = trackPreferences.trackToken(this).get().isNotEmpty()

    override val isLoggedInFlow: Flow<Boolean> by lazy {
        trackPreferences.trackToken(this).changes().map { it.isNotEmpty() }
    }

    class MangaDexNotFoundException : Exception("Mangadex not enabled")

    // KMK -->
    override fun hasNotStartedWatching(status: Long): Boolean = status == FollowStatus.PLAN_TO_READ.long
    // KMK <--
}
