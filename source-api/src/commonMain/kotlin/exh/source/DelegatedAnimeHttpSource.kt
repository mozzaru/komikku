package exh.source

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.Page
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable

@Suppress("OverridingDeprecatedMember", "DEPRECATION")
abstract class DelegatedAnimeHttpSource(val delegate: AnimeHttpSource) : AnimeHttpSource() {
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
     * Returns an observable containing a page with a list of manga. Normally it's not needed to
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
     * Returns an observable containing a page with a list of manga. Normally it's not needed to
     * override this method.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getSearchAnime"))
    override fun fetchSearchAnime(page: Int, query: String, filters: AnimeFilterList): Observable<AnimesPage> {
        ensureDelegateCompatible()
        return delegate.fetchSearchAnime(page, query, filters)
    }

    override suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList): AnimesPage {
        ensureDelegateCompatible()
        return delegate.getSearchAnime(page, query, filters)
    }

    /**
     * Returns an observable containing a page with a list of latest manga updates.
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
     * Returns an observable with the updated details for a manga. Normally it's not needed to
     * override this method.
     *
     * @param anime the manga to be updated.
     */
    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getAnimeDetails"))
    override fun fetchAnimeDetails(anime: SAnime): Observable<SAnime> {
        ensureDelegateCompatible()
        return delegate.fetchAnimeDetails(anime)
    }

    /**
     * [1.x API] Get the updated details for a manga.
     */
    override suspend fun getAnimeDetails(anime: SAnime): SAnime {
        ensureDelegateCompatible()
        return delegate.getAnimeDetails(anime)
    }

    /**
     * Returns the request for the details of a manga. Override only if it's needed to change the
     * url, send different headers or request method like POST.
     *
     * @param anime the manga to be updated.
     */
    override fun animeDetailsRequest(anime: SAnime): Request {
        ensureDelegateCompatible()
        return delegate.animeDetailsRequest(anime)
    }

    /**
     * Returns an observable with the updated chapter list for a manga. Normally it's not needed to
     * override this method.  If a manga is licensed an empty chapter list observable is returned
     *
     * @param anime the manga to look for chapters.
     */
    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getEpisodeList"))
    override fun fetchEpisodeList(anime: SAnime): Observable<List<SEpisode>> {
        ensureDelegateCompatible()
        return delegate.fetchEpisodeList(anime)
    }

    /**
     * [1.x API] Get all the available chapters for a manga.
     */
    override suspend fun getEpisodeList(anime: SAnime): List<SEpisode> {
        ensureDelegateCompatible()
        return delegate.getEpisodeList(anime)
    }

    /**
     * Returns an observable with the page list for a chapter.
     *
     * @param episode the chapter whose page list has to be fetched.
     */
    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getVideoList"))
    override fun fetchVideoList(episode: SEpisode): Observable<List<Page>> {
        ensureDelegateCompatible()
        return delegate.fetchVideoList(episode)
    }

    /**
     * [1.x API] Get the list of pages a chapter has.
     */
    override suspend fun getVideoList(episode: SEpisode): List<Page> {
        ensureDelegateCompatible()
        return delegate.getVideoList(episode)
    }

    /**
     * Returns an observable with the page containing the source url of the image. If there's any
     * error, it will return null instead of throwing an exception.
     *
     * @param video the page whose source image has to be fetched.
     */
    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getImageUrl"))
    override fun fetchImageUrl(video: Page): Observable<String> {
        ensureDelegateCompatible()
        return delegate.fetchImageUrl(video)
    }

    override suspend fun getImageUrl(video: Page): String {
        ensureDelegateCompatible()
        return delegate.getImageUrl(video)
    }

    /**
     * Returns the response of the source image.
     *
     * @param video the page whose source image has to be downloaded.
     */
    override suspend fun getImage(video: Page): Response {
        ensureDelegateCompatible()
        return delegate.getImage(video)
    }

    /**
     * Returns the url of the provided manga
     *
     * @since extensions-lib 1.4
     * @param anime the manga
     * @return url of the manga
     */
    override fun getAnimeUrl(anime: SAnime): String {
        ensureDelegateCompatible()
        return delegate.getAnimeUrl(anime)
    }

    /**
     * Returns the url of the provided chapter
     *
     * @since extensions-lib 1.4
     * @param episode the chapter
     * @return url of the chapter
     */
    override fun getEpisodeUrl(episode: SEpisode): String {
        ensureDelegateCompatible()
        return delegate.getEpisodeUrl(episode)
    }

    /**
     * Called before inserting a new chapter into database. Use it if you need to override chapter
     * fields, like the title or the chapter number. Do not change anything to [anime].
     *
     * @param episode the chapter to be added.
     * @param anime the manga of the chapter.
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
