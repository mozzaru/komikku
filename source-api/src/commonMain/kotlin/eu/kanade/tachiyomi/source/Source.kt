package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.Video
import eu.kanade.tachiyomi.source.model.SAnime
import eu.kanade.tachiyomi.source.model.SEpisode

/**
 * A basic interface for creating a source. It could be an online source, a local source, stub source, etc.
 *
 * Supposedly, it expects extensions to overwrite get...() methods while leaving those fetch...() alone.
 * Hence in extensions-lib, it will leave get...() methods as unimplemented
 * and fetch...() as IllegalStateException("Not used").
 *
 * Prior to extensions-lib 1.5, all extensions still using fetch...(). Because of this,
 * in extensions-lib all get...() methods will be implemented as Exception("Stub!") while
 * all fetch...() methods will leave unimplemented.
 * But if we want to migrate extensions to use get...() then those fetch...()
 * should still be implemented as IllegalStateException("Not used").
 */
interface Source {

    /**
     * ID for the source. Must be unique.
     */
    val id: Long

    /**
     * Name of the source.
     */
    val name: String

    val lang: String
        get() = ""

    /**
     * Get the updated details for a anime.
     *
     * @since extensions-lib 1.4
     * @param anime the anime to update.
     * @return the updated anime.
     */
    suspend fun getAnimeDetails(anime: SAnime): SAnime = throw UnsupportedOperationException()

    /**
     * Get all the available episodes for a anime.
     *
     * @since extensions-lib 1.4
     * @param anime the anime to update.
     * @return the episodes for the anime.
     */
    suspend fun getEpisodeList(anime: SAnime): List<SEpisode> = throw UnsupportedOperationException()

    /**
     * Get the list of videos a episode has. Videos should be returned
     * in the expected order; the index is ignored.
     *
     * @since komikku/extensions-lib 1.7
     * @param episode the episode.
     * @return the videos for the episode.
     */
    suspend fun getVideoList(episode: SEpisode): List<Video> = throw UnsupportedOperationException()

    // KMK -->
    /**
     * Get all the available related animes for a anime.
     *
     * @since komikku/extensions-lib 1.6
     * @param anime the current anime to get related animes.
     * @return a list of <keyword, related animes>
     */
    suspend fun getRelatedAnimeList(
        anime: SAnime,
        exceptionHandler: (Throwable) -> Unit,
        pushResults: suspend (relatedAnime: Pair<String, List<SAnime>>, completed: Boolean) -> Unit,
    ): Unit = throw UnsupportedOperationException()
    // KMK <--
}
