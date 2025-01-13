package exh.source

import eu.kanade.tachiyomi.source.model.AnimesPage
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SAnime
import eu.kanade.tachiyomi.source.model.SEpisode
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable

@Suppress("OverridingDeprecatedMember", "DEPRECATION")
abstract class DelegatedHttpSource(val delegate: HttpSource) : HttpSource() {
    /**
     * Returns the request for the popular anime given the page.
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
     * Returns the request for the search anime given the page.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    override fun searchAnimeRequest(page: Int, query: String, filters: FilterList) =
        throw UnsupportedOperationException("Should never be called!")

    /**
     * Parses the response from the site and returns a [AnimesPage] object.
     *
     * @param response the response from the site.
     */
    override fun searchAnimeParse(response: Response) =
        throw UnsupportedOperationException("Should never be called!")

    /**
     * Returns the request for latest anime given the page.
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
     * Parses the response from the site and returns the details of a anime.
     *
     * @param response the response from the site.
     */
    override fun animeDetailsParse(response: Response) =
        throw UnsupportedOperationException("Should never be called!")

    /**
     * Parses the response from the site and returns a list of episodes.
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
    override fun episodePageParse(response: Response) =
        throw UnsupportedOperationException("Should never be called!")

    /**
     * Parses the response from the site and returns a list of pages.
     *
     * @param response the response from the site.
     */
    override fun pageListParse(response: Response) =
        throw UnsupportedOperationException("Should never be called!")

    /**
     * Parses the response from the site and returns the absolute url to the source image.
     *
     * @param response the response from the site.
     */
    override fun imageUrlParse(response: Response) =
        throw UnsupportedOperationException("Should never be called!")

    /**
     * Base url of the website without the trailing slash, like: http://mysite.com
     */
    override val baseUrl get() = delegate.baseUrl

    /**
     * Headers used for requests.
     */
    override val headers get() = delegate.headers

    /**
     * Whether the source has support for latest updates.
     */
    override val supportsLatest get() = delegate.supportsLatest

    /**
     * Name of the source.
     */
    final override val name get() = delegate.name

    // ===> OPTIONAL FIELDS

    /**
     * Id of the source. By default it uses a generated id using the first 16 characters (64 bits)
     * of the MD5 of the string: sourcename/language/versionId
     * Note the generated id sets the sign bit to 0.
     */
    override val id get() = delegate.id

    /**
     * Default network client for doing requests.
     */
    final override val client get() = delegate.client

    /**
     * You must NEVER call super.client if you override this!
     */
    open val baseHttpClient: OkHttpClient? = null
    open val networkHttpClient: OkHttpClient get() = network.client

    /**
     * Visible name of the source.
     */
    override fun toString() = delegate.toString()

    /**
     * Returns an observable containing a page with a list of anime. Normally it's not needed to
     * override this method.
     *
     * @param page the page number to retrieve.
     */
    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getPopularAnime"))
    override fun fetchPopularAnime(page: Int): Observable<AnimesPage> {
        ensureDelegateCompatible()
        return delegate.fetchPopularAnime(page)
    }

    override suspend fun getPopularAnime(page: Int): AnimesPage {
        ensureDelegateCompatible()
        return delegate.getPopularAnime(page)
    }

    /**
     * Returns an observable containing a page with a list of anime. Normally it's not needed to
     * override this method.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getSearchAnime"))
    override fun fetchSearchAnime(page: Int, query: String, filters: FilterList): Observable<AnimesPage> {
        ensureDelegateCompatible()
        return delegate.fetchSearchAnime(page, query, filters)
    }

    override suspend fun getSearchAnime(page: Int, query: String, filters: FilterList): AnimesPage {
        ensureDelegateCompatible()
        return delegate.getSearchAnime(page, query, filters)
    }

    /**
     * Returns an observable containing a page with a list of latest anime updates.
     *
     * @param page the page number to retrieve.
     */
    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getLatestUpdates"))
    override fun fetchLatestUpdates(page: Int): Observable<AnimesPage> {
        ensureDelegateCompatible()
        return delegate.fetchLatestUpdates(page)
    }

    override suspend fun getLatestUpdates(page: Int): AnimesPage {
        ensureDelegateCompatible()
        return delegate.getLatestUpdates(page)
    }

    /**
     * Returns an observable with the updated details for a anime. Normally it's not needed to
     * override this method.
     *
     * @param anime the anime to be updated.
     */
    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getAnimeDetails"))
    override fun fetchAnimeDetails(anime: SAnime): Observable<SAnime> {
        ensureDelegateCompatible()
        return delegate.fetchAnimeDetails(anime)
    }

    /**
     * [1.x API] Get the updated details for a anime.
     */
    override suspend fun getAnimeDetails(anime: SAnime): SAnime {
        ensureDelegateCompatible()
        return delegate.getAnimeDetails(anime)
    }

    /**
     * Returns the request for the details of a anime. Override only if it's needed to change the
     * url, send different headers or request method like POST.
     *
     * @param anime the anime to be updated.
     */
    override fun animeDetailsRequest(anime: SAnime): Request {
        ensureDelegateCompatible()
        return delegate.animeDetailsRequest(anime)
    }

    /**
     * Returns an observable with the updated episode list for a anime. Normally it's not needed to
     * override this method.  If a anime is licensed an empty episode list observable is returned
     *
     * @param anime the anime to look for episodes.
     */
    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getEpisodeList"))
    override fun fetchEpisodeList(anime: SAnime): Observable<List<SEpisode>> {
        ensureDelegateCompatible()
        return delegate.fetchEpisodeList(anime)
    }

    /**
     * [1.x API] Get all the available episodes for a anime.
     */
    override suspend fun getEpisodeList(anime: SAnime): List<SEpisode> {
        ensureDelegateCompatible()
        return delegate.getEpisodeList(anime)
    }

    /**
     * Returns an observable with the page list for a episode.
     *
     * @param episode the episode whose page list has to be fetched.
     */
    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getPageList"))
    override fun fetchPageList(episode: SEpisode): Observable<List<Page>> {
        ensureDelegateCompatible()
        return delegate.fetchPageList(episode)
    }

    /**
     * [1.x API] Get the list of pages a episode has.
     */
    override suspend fun getPageList(episode: SEpisode): List<Page> {
        ensureDelegateCompatible()
        return delegate.getPageList(episode)
    }

    /**
     * Returns an observable with the page containing the source url of the image. If there's any
     * error, it will return null instead of throwing an exception.
     *
     * @param page the page whose source image has to be fetched.
     */
    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getImageUrl"))
    override fun fetchImageUrl(page: Page): Observable<String> {
        ensureDelegateCompatible()
        return delegate.fetchImageUrl(page)
    }

    override suspend fun getImageUrl(page: Page): String {
        ensureDelegateCompatible()
        return delegate.getImageUrl(page)
    }

    /**
     * Returns the response of the source image.
     *
     * @param page the page whose source image has to be downloaded.
     */
    override suspend fun getImage(page: Page): Response {
        ensureDelegateCompatible()
        return delegate.getImage(page)
    }

    /**
     * Returns the url of the provided anime
     *
     * @since extensions-lib 1.4
     * @param anime the anime
     * @return url of the anime
     */
    override fun getAnimeUrl(anime: SAnime): String {
        ensureDelegateCompatible()
        return delegate.getAnimeUrl(anime)
    }

    /**
     * Returns the url of the provided episode
     *
     * @since extensions-lib 1.4
     * @param episode the episode
     * @return url of the episode
     */
    override fun getEpisodeUrl(episode: SEpisode): String {
        ensureDelegateCompatible()
        return delegate.getEpisodeUrl(episode)
    }

    /**
     * Called before inserting a new episode into database. Use it if you need to override episode
     * fields, like the title or the episode number. Do not change anything to [anime].
     *
     * @param episode the episode to be added.
     * @param anime the anime of the episode.
     */
    override fun prepareNewEpisode(episode: SEpisode, anime: SAnime) {
        ensureDelegateCompatible()
        return delegate.prepareNewEpisode(episode, anime)
    }

    /**
     * Returns the list of filters for the source.
     */
    override fun getFilterList() = delegate.getFilterList()

    protected open fun ensureDelegateCompatible() {
        if (versionId != delegate.versionId || lang != delegate.lang) {
            throw IncompatibleDelegateException(
                "Delegate source is not compatible (" +
                    "versionId: $versionId <=> ${delegate.versionId}, lang: $lang <=> ${delegate.lang}" +
                    ")!",
            )
        }
    }

    class IncompatibleDelegateException(message: String) : RuntimeException(message)

    init {
        delegate.bindDelegate(this)
    }
}
