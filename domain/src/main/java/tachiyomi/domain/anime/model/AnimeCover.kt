package tachiyomi.domain.anime.model

import tachiyomi.domain.anime.interactor.GetCustomAnimeInfo
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.ConcurrentHashMap

/**
 * Contains the required data for AnimeCoverFetcher
 */
data class AnimeCover(
    val animeId: Long,
    val sourceId: Long,
    val isAnimeFavorite: Boolean,
    // SY -->
    val ogUrl: String?,
    // SY <--
    val lastModified: Long,
) {
    // SY -->
    private val customThumbnailUrl = if (isAnimeFavorite) {
        getCustomAnimeInfo.get(animeId)?.thumbnailUrl
    } else {
        null
    }
    val url: String? = customThumbnailUrl ?: ogUrl
    // SY <--

    // KMK -->
    /**
     * [vibrantCoverColor] is used to set the color theme in anime detail page.
     * It contains color for all animes, both in library or browsing.
     *
     * It reads/saves to a hashmap in [AnimeCover.vibrantCoverColorMap] for multiple animes.
     */
    var vibrantCoverColor: Int?
        get() = vibrantCoverColorMap[animeId]
        set(value) {
            vibrantCoverColorMap[animeId] = value
        }

    /**
     * [dominantCoverColors] is used to set cover/text's color in Library (Favorite) grid view.
     * It contains only color for in-library (favorite) animes.
     *
     * It reads/saves to a hashmap in [AnimeCover.dominantCoverColorMap].
     *
     * Format: <first: cover color, second: text color>.
     *
     * Set in *[AnimeCoverMetadata.setRatioAndColors]* whenever browsing meets a favorite anime
     *  by loading from *[CoverCache]*.
     *
     * Get in *[CommonAnimeItem.AnimeCompactGridItem]*, *[CommonAnimeItem.AnimeComfortableGridItem]* and
     *  *[CommonAnimeItem.AnimeListItem]*
     */
    @Suppress("KDocUnresolvedReference")
    var dominantCoverColors: Pair<Int, Int>?
        get() = dominantCoverColorMap[animeId]
        set(value) {
            value ?: return
            dominantCoverColorMap[animeId] = value.first to value.second
        }

    var ratio: Float?
        get() = coverRatioMap[animeId]
        set(value) {
            value ?: return
            coverRatioMap[animeId] = value
        }

    companion object {
        /**
         * [vibrantCoverColorMap] store color generated while browsing library.
         * It always empty at beginning each time app starts, then add more color while browsing.
         */
        val vibrantCoverColorMap: HashMap<Long, Int?> = hashMapOf()

        /**
         * [dominantCoverColorMap] stores favorite anime's cover & text's color as a joined string in Prefs.
         * They will be loaded each time *[App]* is initialized with *[AnimeCoverMetadata.load]*.
         *
         * They will be saved back when *[MainActivity.onPause]* is triggered.
         */
        @Suppress("KDocUnresolvedReference")
        var dominantCoverColorMap = ConcurrentHashMap<Long, Pair<Int, Int>>()

        var coverRatioMap = ConcurrentHashMap<Long, Float>()
        // KMK <--

        // SY -->
        private val getCustomAnimeInfo: GetCustomAnimeInfo by injectLazy()
        // SY <--
    }
}

fun Anime.asAnimeCover(): AnimeCover {
    return AnimeCover(
        animeId = id,
        sourceId = source,
        isAnimeFavorite = favorite,
        ogUrl = thumbnailUrl,
        lastModified = coverLastModified,
    )
}
