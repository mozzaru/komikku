package tachiyomi.domain.anime.model

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.anime.interactor.GetCustomAnimeInfo
import uy.kohesive.injekt.injectLazy
import java.io.Serializable
import java.time.Instant

@Immutable
data class Anime(
    val id: Long,
    val source: Long,
    val favorite: Boolean,
    val lastUpdate: Long,
    val nextUpdate: Long,
    val fetchInterval: Int,
    val dateAdded: Long,
    val viewerFlags: Long,
    val episodeFlags: Long,
    val coverLastModified: Long,
    val url: String,
    // SY -->
    val ogTitle: String,
    val ogArtist: String?,
    val ogAuthor: String?,
    val ogThumbnailUrl: String?,
    val ogDescription: String?,
    val ogGenre: List<String>?,
    val ogStatus: Long,
    // SY <--
    val updateStrategy: UpdateStrategy,
    val initialized: Boolean,
    val lastModifiedAt: Long,
    val favoriteModifiedAt: Long?,
    val version: Long,
) : Serializable {

    // SY -->
    private val customAnimeInfo = if (favorite) {
        getCustomAnimeInfo.get(id)
    } else {
        null
    }

    val title: String
        get() = customAnimeInfo?.title ?: ogTitle

    val author: String?
        get() = customAnimeInfo?.author ?: ogAuthor

    val artist: String?
        get() = customAnimeInfo?.artist ?: ogArtist

    val thumbnailUrl: String?
        get() = customAnimeInfo?.thumbnailUrl ?: ogThumbnailUrl

    val description: String?
        get() = customAnimeInfo?.description ?: ogDescription

    val genre: List<String>?
        get() = customAnimeInfo?.genre ?: ogGenre

    val status: Long
        get() = customAnimeInfo?.status ?: ogStatus
    // SY <--

    val expectedNextUpdate: Instant?
        get() = nextUpdate
            /* KMK -->
            Always predict release date even for Completed entries
            .takeIf { status != SAnime.COMPLETED.toLong() }?
             KMK <-- */
            .let { Instant.ofEpochMilli(it) }

    val sorting: Long
        get() = episodeFlags and EPISODE_SORTING_MASK

    val displayMode: Long
        get() = episodeFlags and EPISODE_DISPLAY_MASK

    val unseenFilterRaw: Long
        get() = episodeFlags and EPISODE_UNSEEN_MASK

    val downloadedFilterRaw: Long
        get() = episodeFlags and EPISODE_DOWNLOADED_MASK

    val bookmarkedFilterRaw: Long
        get() = episodeFlags and EPISODE_BOOKMARKED_MASK

    val unseenFilter: TriState
        get() = when (unseenFilterRaw) {
            EPISODE_SHOW_UNSEEN -> TriState.ENABLED_IS
            EPISODE_SHOW_SEEN -> TriState.ENABLED_NOT
            else -> TriState.DISABLED
        }

    val bookmarkedFilter: TriState
        get() = when (bookmarkedFilterRaw) {
            EPISODE_SHOW_BOOKMARKED -> TriState.ENABLED_IS
            EPISODE_SHOW_NOT_BOOKMARKED -> TriState.ENABLED_NOT
            else -> TriState.DISABLED
        }

    fun sortDescending(): Boolean {
        return episodeFlags and EPISODE_SORT_DIR_MASK == EPISODE_SORT_DESC
    }

    companion object {
        // Generic filter that does not filter anything
        const val SHOW_ALL = 0x00000000L

        const val EPISODE_SORT_DESC = 0x00000000L
        const val EPISODE_SORT_ASC = 0x00000001L
        const val EPISODE_SORT_DIR_MASK = 0x00000001L

        const val EPISODE_SHOW_UNSEEN = 0x00000002L
        const val EPISODE_SHOW_SEEN = 0x00000004L
        const val EPISODE_UNSEEN_MASK = 0x00000006L

        const val EPISODE_SHOW_DOWNLOADED = 0x00000008L
        const val EPISODE_SHOW_NOT_DOWNLOADED = 0x00000010L
        const val EPISODE_DOWNLOADED_MASK = 0x00000018L

        const val EPISODE_SHOW_BOOKMARKED = 0x00000020L
        const val EPISODE_SHOW_NOT_BOOKMARKED = 0x00000040L
        const val EPISODE_BOOKMARKED_MASK = 0x00000060L

        const val EPISODE_SORTING_SOURCE = 0x00000000L
        const val EPISODE_SORTING_NUMBER = 0x00000100L
        const val EPISODE_SORTING_UPLOAD_DATE = 0x00000200L
        const val EPISODE_SORTING_ALPHABET = 0x00000300L
        const val EPISODE_SORTING_MASK = 0x00000300L

        const val EPISODE_DISPLAY_NAME = 0x00000000L
        const val EPISODE_DISPLAY_NUMBER = 0x00100000L
        const val EPISODE_DISPLAY_MASK = 0x00100000L

        fun create() = Anime(
            id = -1L,
            url = "",
            // Sy -->
            ogTitle = "",
            // SY <--
            source = -1L,
            favorite = false,
            lastUpdate = 0L,
            nextUpdate = 0L,
            fetchInterval = 0,
            dateAdded = 0L,
            viewerFlags = 0L,
            episodeFlags = 0L,
            coverLastModified = 0L,
            // SY -->
            ogArtist = null,
            ogAuthor = null,
            ogThumbnailUrl = null,
            ogDescription = null,
            ogGenre = null,
            ogStatus = 0L,
            // SY <--
            updateStrategy = UpdateStrategy.ALWAYS_UPDATE,
            initialized = false,
            lastModifiedAt = 0L,
            favoriteModifiedAt = null,
            version = 0L,
        )

        // SY -->
        private val getCustomAnimeInfo: GetCustomAnimeInfo by injectLazy()
        // SY <--
    }
}
