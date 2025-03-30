package eu.kanade.tachiyomi.source

import dev.icerock.moko.graphics.BuildConfig
import eu.kanade.tachiyomi.source.model.AnimesPage
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SAnime
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * A basic interface for creating a source. It could be an online source, a local source, etc.
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
interface CatalogueSource : Source {

    /**
     * An ISO 639-1 compliant language code (two letters in lower case).
     */
    override val lang: String

    /**
     * Whether the source has support for latest updates.
     */
    val supportsLatest: Boolean

    /**
     * Get a page with a list of anime.
     *
     * @since extensions-lib 1.5
     * @param page the page number to retrieve.
     */
    suspend fun getPopularAnime(page: Int): AnimesPage = throw UnsupportedOperationException()

    /**
     * Get a page with a list of anime.
     *
     * @since extensions-lib 1.5
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    suspend fun getSearchAnime(page: Int, query: String, filters: FilterList): AnimesPage = throw UnsupportedOperationException()

    /**
     * Get a page with a list of latest anime updates.
     *
     * @since extensions-lib 1.5
     * @param page the page number to retrieve.
     */
    suspend fun getLatestUpdates(page: Int): AnimesPage

    /**
     * Returns the list of filters for the source.
     */
    fun getFilterList(): FilterList

    // KMK -->
    /**
     * Whether parsing related animes in anime page or extension provide custom related animes request.
     * @default false
     * @since komikku/extensions-lib 1.6
     */
    val supportsRelatedAnimes: Boolean get() = false

    /**
     * Extensions doesn't want to use App's [getRelatedAnimeListBySearch].
     * @default false
     * @since komikku/extensions-lib 1.6
     */
    val disableRelatedAnimesBySearch: Boolean get() = false

    /**
     * Disable showing any related animes.
     * @default false
     * @since komikku/extensions-lib 1.6
     */
    val disableRelatedAnimes: Boolean get() = false

    /**
     * Get all the available related animes for a anime.
     * Normally it's not needed to override this method.
     *
     * @since komikku/extensions-lib 1.6
     * @param anime the current anime to get related animes.
     * @return a list of <keyword, related animes>
     * @throws UnsupportedOperationException if a source doesn't support related animes.
     */
    override suspend fun getRelatedAnimeList(
        anime: SAnime,
        exceptionHandler: (Throwable) -> Unit,
        pushResults: suspend (relatedAnime: Pair<String, List<SAnime>>, completed: Boolean) -> Unit,
    ) {
        val handler = CoroutineExceptionHandler { _, e -> exceptionHandler(e) }
        if (!disableRelatedAnimes) {
            supervisorScope {
                if (this@CatalogueSource.supportsRelatedAnimes) launch(handler) { this@CatalogueSource.getRelatedAnimeListByExtension(anime, pushResults) }
                if (!this@CatalogueSource.disableRelatedAnimesBySearch) launch(handler) { this@CatalogueSource.getRelatedAnimeListBySearch(anime, pushResults) }
            }
        }
    }

    /**
     * Get related animes provided by extension
     *
     * @return a list of <keyword, related animes>
     * @since komikku/extensions-lib 1.6
     */
    suspend fun getRelatedAnimeListByExtension(
        anime: SAnime,
        pushResults: suspend (relatedAnime: Pair<String, List<SAnime>>, completed: Boolean) -> Unit,
    ) {
        runCatching { this.fetchRelatedAnimeList(anime) }
            .onSuccess { if (it.isNotEmpty()) pushResults(Pair("", it), false) }
            .onFailure { e ->
                @Suppress("KotlinConstantConditions")
                if (BuildConfig.BUILD_TYPE == "release") {
                    logcat(LogPriority.ERROR, e) { "## getRelatedAnimeListByExtension: $e" }
                } else {
                    throw UnsupportedOperationException(
                        "Extension doesn't support site's related entries," +
                            " please report an issue to Komikku.",
                    )
                }
            }
    }

    /**
     * Fetch related animes for a anime from source/site.
     *
     * @since komikku/extensions-lib 1.6
     * @param anime the current anime to get related animes.
     * @return the related animes for the current anime.
     * @throws UnsupportedOperationException if a source doesn't support related animes.
     */
    suspend fun fetchRelatedAnimeList(anime: SAnime): List<SAnime> = throw UnsupportedOperationException("Unsupported!")

    /**
     * Slit & strip anime's title into separate searchable keywords.
     * Used for searching related animes.
     *
     * @since komikku/extensions-lib 1.6
     * @return List of keywords.
     */
    fun String.stripKeywordForRelatedAnimes(): List<String> {
        val regexWhitespace = Regex("\\s+")
        val regexSpecialCharacters =
            Regex("([!~#$%^&*+_|/\\\\,?:;'“”‘’\"<>(){}\\[\\]。・～：—！？、―«»《》〘〙【】「」｜]|\\s-|-\\s|\\s\\.|\\.\\s)")
        val regexNumberOnly = Regex("^\\d+$")

        return replace(regexSpecialCharacters, " ")
            .split(regexWhitespace)
            .map {
                // remove number only
                it.replace(regexNumberOnly, "")
                    .lowercase()
            }
            // exclude single character
            .filter { it.length > 1 }
    }

    /**
     * Get related animes by searching for each keywords from anime's title.
     *
     * @return a list of <keyword, related animes>
     * @since komikku/extensions-lib 1.6
     */
    suspend fun getRelatedAnimeListBySearch(
        anime: SAnime,
        pushResults: suspend (relatedAnime: Pair<String, List<SAnime>>, completed: Boolean) -> Unit,
    ) {
        val words = HashSet<String>()
        words.add(anime.title)
        if (anime.title.lowercase() != anime.originalTitle.lowercase()) words.add(anime.originalTitle)
        anime.title.stripKeywordForRelatedAnimes()
            .filterNot { word -> words.any { it.lowercase() == word } }
            .onEach { words.add(it) }
        anime.originalTitle.stripKeywordForRelatedAnimes()
            .filterNot { word -> words.any { it.lowercase() == word } }
            .onEach { words.add(it) }
        if (words.isEmpty()) return

        coroutineScope {
            words.map { keyword ->
                launch {
                    runCatching {
                        this@CatalogueSource.getSearchAnime(1, keyword, FilterList()).animes
                    }
                        .onSuccess { if (it.isNotEmpty()) pushResults(Pair(keyword, it), false) }
                        .onFailure { e ->
                            logcat(LogPriority.ERROR, e) { "## getRelatedAnimeListBySearch: $e" }
                        }
                }
            }
        }
    }
    // KMK <--
}
