package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.model.AnimesPage
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SAnime
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SEpisode
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.Video
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * A simple implementation for sources from a website using Jsoup, an HTML parser.
 */
@Suppress("unused")
abstract class ParsedHttpSource : HttpSource() {

    /**
     * Parses the response from the site and returns a [AnimesPage] object.
     * Normally it's not needed to override this method.
     *
     * @param response the response from the site.
     */
    override fun popularAnimeParse(response: Response): AnimesPage {
        val document = response.asJsoup()

        val animes = document.select(popularAnimeSelector()).map { element ->
            popularAnimeFromElement(element)
        }

        val hasNextPage = popularAnimeNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return AnimesPage(animes, hasNextPage)
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each anime.
     */
    protected open fun popularAnimeSelector(): String = popularMangaSelector()
    protected abstract fun popularMangaSelector(): String

    /**
     * Returns a anime from the given [element]. Most sites only show the title and the url, it's
     * totally fine to fill only those two values.
     *
     * @param element an element obtained from [popularAnimeSelector].
     */
    protected open fun popularAnimeFromElement(element: Element): SAnime = popularMangaFromElement(element)
    protected abstract fun popularMangaFromElement(element: Element): SManga

    /**
     * Returns the Jsoup selector that returns the <a> tag linking to the next page, or null if
     * there's no next page.
     */
    protected open fun popularAnimeNextPageSelector(): String? = popularMangaNextPageSelector()
    protected abstract fun popularMangaNextPageSelector(): String?

    /**
     * Parses the response from the site and returns a [AnimesPage] object.
     * Normally it's not needed to override this method.
     *
     * @param response the response from the site.
     */
    override fun searchAnimeParse(response: Response): AnimesPage = searchMangaParse(response)
    override fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val animes = document.select(searchAnimeSelector()).map { element ->
            searchAnimeFromElement(element)
        }

        val hasNextPage = searchAnimeNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return AnimesPage(animes, hasNextPage)
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each anime.
     */
    protected open fun searchAnimeSelector(): String = searchMangaSelector()
    protected abstract fun searchMangaSelector(): String

    /**
     * Returns a anime from the given [element]. Most sites only show the title and the url, it's
     * totally fine to fill only those two values.
     *
     * @param element an element obtained from [searchAnimeSelector].
     */
    protected open fun searchAnimeFromElement(element: Element): SAnime = searchMangaFromElement(element)
    protected abstract fun searchMangaFromElement(element: Element): SManga

    /**
     * Returns the Jsoup selector that returns the <a> tag linking to the next page, or null if
     * there's no next page.
     */
    protected open fun searchAnimeNextPageSelector(): String? = searchMangaNextPageSelector()
    protected abstract fun searchMangaNextPageSelector(): String?

    /**
     * Parses the response from the site and returns a [AnimesPage] object.
     * Normally it's not needed to override this method.
     *
     * @param response the response from the site.
     */
    override fun latestUpdatesParse(response: Response): AnimesPage {
        val document = response.asJsoup()

        val animes = document.select(latestUpdatesSelector()).map { element ->
            latestUpdatesFromElement(element)
        }

        val hasNextPage = latestUpdatesNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return AnimesPage(animes, hasNextPage)
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each anime.
     */
    protected abstract fun latestUpdatesSelector(): String

    /**
     * Returns a anime from the given [element]. Most sites only show the title and the url, it's
     * totally fine to fill only those two values.
     *
     * @param element an element obtained from [latestUpdatesSelector].
     */
    protected abstract fun latestUpdatesFromElement(element: Element): SAnime

    /**
     * Returns the Jsoup selector that returns the <a> tag linking to the next page, or null if
     * there's no next page.
     */
    protected abstract fun latestUpdatesNextPageSelector(): String?

    /**
     * Parses the response from the site and returns the details of a anime.
     * Normally it's not needed to override this method.
     *
     * @param response the response from the site.
     */
    override fun animeDetailsParse(response: Response): SAnime = mangaDetailsParse(response)
    override fun mangaDetailsParse(response: Response): SAnime {
        return animeDetailsParse(response.asJsoup())
    }

    /**
     * Returns the details of the anime from the given [document].
     *
     * @param document the parsed document.
     */
    protected open fun animeDetailsParse(document: Document): SAnime = mangaDetailsParse(document)
    protected abstract fun mangaDetailsParse(document: Document): SManga

    // KMK -->
    /**
     * Parses the response from the site and returns a list of related animes.
     * Normally it's not needed to override this method.
     *
     * @since komikku/extensions-lib 1.6
     * @param response the response from the site.
     */
    override fun relatedAnimeListParse(response: Response): List<SAnime> = relatedMangaListParse(response)
    override fun relatedMangaListParse(response: Response): List<SManga> {
        return response.asJsoup()
            .select(relatedAnimeListSelector()).map { relatedAnimeFromElement(it) }
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each related animes.
     *
     * @since komikku/extensions-lib 1.6
     */
    protected open fun relatedAnimeListSelector(): String = relatedMangaListSelector()
    protected open fun relatedMangaListSelector(): String = popularAnimeSelector()

    /**
     * Returns a anime from the given element.
     *
     * @since komikku/extensions-lib 1.6
     * @param element an element obtained from [relatedAnimeListSelector].
     */
    protected open fun relatedAnimeFromElement(element: Element): SAnime = relatedAnimeFromElement(element)
    protected open fun relatedMangaFromElement(element: Element): SManga = popularAnimeFromElement(element)
    // KMK <--

    /**
     * Parses the response from the site and returns a list of episodes.
     * Normally it's not needed to override this method.
     *
     * @param response the response from the site.
     */
    override fun episodeListParse(response: Response): List<SEpisode> = chapterListParse(response)
    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        return document.select(episodeListSelector()).map { episodeFromElement(it) }
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each episode.
     */
    protected open fun episodeListSelector(): String = chapterListSelector()
    protected abstract fun chapterListSelector(): String

    /**
     * Returns a episode from the given element.
     *
     * @param element an element obtained from [episodeListSelector].
     */
    protected open fun episodeFromElement(element: Element): SEpisode = chapterFromElement(element)
    protected abstract fun chapterFromElement(element: Element): SChapter

    /**
     * Parses the response from the site and returns the video list.
     * Normally it's not needed to override this method.
     *
     * @param response the response from the site.
     */
    override fun videoListParse(response: Response): List<Video> = pageListParse(response)
    override fun pageListParse(response: Response): List<Page> {
        return videoListParse(response.asJsoup())
    }

    /**
     * Returns a video list from the given document.
     *
     * @param document the parsed document.
     */
    protected open fun videoListParse(document: Document): List<Video> = pageListParse(document)
    protected abstract fun pageListParse(document: Document): List<Page>

    /**
     * Parse the response from the site and returns the absolute url to the source video.
     * Normally it's not needed to override this method.
     *
     * @param response the response from the site.
     */
    override fun videoUrlParse(response: Response): String = imageUrlParse(response)
    override fun imageUrlParse(response: Response): String {
        return videoUrlParse(response.asJsoup())
    }

    /**
     * Returns the absolute url to the source video from the document.
     *
     * @param document the parsed document.
     */
    protected open fun videoUrlParse(document: Document): String = imageUrlParse(document)
    protected abstract fun imageUrlParse(document: Document): String
}
