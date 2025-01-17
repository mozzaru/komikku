package exh.source

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.Page
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import exh.pref.DelegateSourcePreferences
import okhttp3.Response
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Suppress("OverridingDeprecatedMember", "DEPRECATION")
class EnhancedAnimeHttpSource(
    val originalSource: AnimeHttpSource,
    val enhancedSource: AnimeHttpSource,
) : AnimeHttpSource() {

    /**
     * Returns the request for the popular manga given the page.
     *
     * @param page the page number to retrieve.
     */
    override fun popularAnimeRequest(page: Int) =
        throw UnsupportedOperationException("Should never be called!")

    /**
     * Parses the response from the site and returns a [AnimesPage] object.
     *
     * @param response the response from the site.
     */
    override fun popularAnimeParse(response: Response) =
        throw UnsupportedOperationException("Should never be called!")

    /**
     * Returns the request for the search manga given the page.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList) =
        throw UnsupportedOperationException("Should never be called!")

    /**
     * Parses the response from the site and returns a [AnimesPage] object.
     *
     * @param response the response from the site.
     */
    override fun searchAnimeParse(response: Response) =
        throw UnsupportedOperationException("Should never be called!")

    /**
     * Returns the request for latest manga given the page.
     *
     * @param page the page number to retrieve.
     */
    override fun latestUpdatesRequest(page: Int) =
        throw UnsupportedOperationException("Should never be called!")

    /**
     * Parses the response from the site and returns a [AnimesPage] object.
     *
     * @param response the response from the site.
     */
    override fun latestUpdatesParse(response: Response) =
        throw UnsupportedOperationException("Should never be called!")

    /**
     * Parses the response from the site and returns the details of a manga.
     *
     * @param response the response from the site.
     */
    override fun animeDetailsParse(response: Response) =
        throw UnsupportedOperationException("Should never be called!")

    /**
     * Parses the response from the site and returns a list of chapters.
     *
     * @param response the response from the site.
     */
    override fun episodeListParse(response: Response) =
        throw UnsupportedOperationException("Should never be called!")

    /**
     * Parses the response from the site and returns a SEpisode Object.
     *
     * @param response the response from the site.
     */
    override fun episodeVideoParse(response: Response) =
        throw UnsupportedOperationException("Should never be called!")

    /**
     * Parses the response from the site and returns a list of pages.
     *
     * @param response the response from the site.
     */
    override fun videoListParse(response: Response) =
        throw UnsupportedOperationException("Should never be called!")

    /**
     * Parses the response from the site and returns the absolute url to the source image.
     *
     * @param response the response from the site.
     */
    override fun videoUrlParse(response: Response) =
        throw UnsupportedOperationException("Should never be called!")

    /**
     * Base url of the website without the trailing slash, like: http://mysite.com
     */
    override val baseUrl get() = source().baseUrl

    /**
     * Headers used for requests.
     */
    override val headers get() = source().headers

    /**
     * Whether the source has support for latest updates.
     */
    override val supportsLatest get() = source().supportsLatest

    /**
     * Name of the source.
     */
    override val name get() = source().name

    /**
     * An ISO 639-1 compliant language code (two letters in lower case).
     */
    override val lang get() = source().lang

    // ===> OPTIONAL FIELDS

    /**
     * Id of the source. By default it uses a generated id using the first 16 characters (64 bits)
     * of the MD5 of the string: sourcename/language/versionId
     * Note the generated id sets the sign bit to 0.
     */
    override val id get() = source().id

    /**
     * Default network client for doing requests.
     */
    override val client get() = originalSource.client // source().client

    /**
     * Visible name of the source.
     */
    override fun toString() = source().toString()

    /**
     * Returns an observable containing a page with a list of manga. Normally it's not needed to
     * override this method.
     *
     * @param page the page number to retrieve.
     */
    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getPopularAnime"))
    override fun fetchPopularAnime(page: Int) = source().fetchPopularAnime(page)

    override suspend fun getPopularAnime(page: Int) = source().getPopularAnime(page)

    /**
     * Returns an observable containing a page with a list of manga. Normally it's not needed to
     * override this method.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getSearchAnime"))
    override fun fetchSearchAnime(page: Int, query: String, filters: AnimeFilterList) =
        source().fetchSearchAnime(page, query, filters)

    override suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList) =
        source().getSearchAnime(page, query, filters)

    /**
     * Returns an observable containing a page with a list of latest manga updates.
     *
     * @param page the page number to retrieve.
     */
    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getLatestUpdates"))
    override fun fetchLatestUpdates(page: Int) = source().fetchLatestUpdates(page)

    override suspend fun getLatestUpdates(page: Int) = source().getLatestUpdates(page)

    /**
     * Returns an observable with the updated details for a manga. Normally it's not needed to
     * override this method.
     *
     * @param anime the manga to be updated.
     */
    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getAnimeDetails"))
    override fun fetchAnimeDetails(anime: SAnime) = source().fetchAnimeDetails(anime)

    /**
     * [1.x API] Get the updated details for a manga.
     */
    override suspend fun getAnimeDetails(anime: SAnime): SAnime = source().getAnimeDetails(anime)

    /**
     * Returns the request for the details of a manga. Override only if it's needed to change the
     * url, send different headers or request method like POST.
     *
     * @param anime the manga to be updated.
     */
    override fun animeDetailsRequest(anime: SAnime) = source().animeDetailsRequest(anime)

    /**
     * Returns an observable with the updated chapter list for a manga. Normally it's not needed to
     * override this method.  If a manga is licensed an empty chapter list observable is returned
     *
     * @param anime the manga to look for chapters.
     */
    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getEpisodeList"))
    override fun fetchEpisodeList(anime: SAnime) = source().fetchEpisodeList(anime)

    /**
     * [1.x API] Get all the available chapters for a manga.
     */
    override suspend fun getEpisodeList(anime: SAnime): List<SEpisode> = source().getEpisodeList(anime)

    /**
     * Returns an observable with the page list for a chapter.
     *
     * @param episode the chapter whose page list has to be fetched.
     */
    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getVideoList"))
    override fun fetchVideoList(episode: SEpisode) = source().fetchVideoList(episode)

    /**
     * [1.x API] Get the list of pages a chapter has.
     */
    override suspend fun getVideoList(episode: SEpisode): List<Page> = source().getVideoList(episode)

    /**
     * Returns an observable with the page containing the source url of the image. If there's any
     * error, it will return null instead of throwing an exception.
     *
     * @param video the page whose source image has to be fetched.
     */
    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getImageUrl"))
    override fun fetchImageUrl(video: Page) = source().fetchImageUrl(video)

    override suspend fun getImageUrl(video: Page) = source().getImageUrl(video)

    /**
     * Returns the response of the source image.
     *
     * @param video the page whose source image has to be downloaded.
     */
    override suspend fun getImage(video: Page) = source().getImage(video)

    /**
     * Returns the url of the provided manga
     *
     * @since extensions-lib 1.4
     * @param anime the manga
     * @return url of the manga
     */
    override fun getAnimeUrl(anime: SAnime) = source().getAnimeUrl(anime)

    /**
     * Returns the url of the provided chapter
     *
     * @since extensions-lib 1.4
     * @param episode the chapter
     * @return url of the chapter
     */
    override fun getEpisodeUrl(episode: SEpisode) = source().getEpisodeUrl(episode)

    /**
     * Called before inserting a new chapter into database. Use it if you need to override chapter
     * fields, like the title or the chapter number. Do not change anything to [anime].
     *
     * @param episode the chapter to be added.
     * @param anime the manga of the chapter.
     */
    override fun prepareNewEpisode(episode: SEpisode, anime: SAnime) =
        source().prepareNewEpisode(episode, anime)

    /**
     * Returns the list of filters for the source.
     */
    override fun getFilterList() = source().getFilterList()

    fun source(): AnimeHttpSource {
        return if (Injekt.get<DelegateSourcePreferences>().delegateSources().get()) {
            enhancedSource
        } else {
            originalSource
        }
    }
}
