package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.model.AnimesPage
import eu.kanade.tachiyomi.source.model.SAnime
import eu.kanade.tachiyomi.source.model.SEpisode
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

        val animes = document.select(this.popularAnimeSelector()).map { element ->
            this.popularAnimeFromElement(element)
        }

        val hasNextPage = this.popularAnimeNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return AnimesPage(animes, hasNextPage)
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each anime.
     */
    protected abstract fun popularAnimeSelector(): String

    /**
     * Returns a anime from the given [element]. Most sites only show the title and the url, it's
     * totally fine to fill only those two values.
     *
     * @param element an element obtained from [popularAnimeSelector].
     */
    protected abstract fun popularAnimeFromElement(element: Element): SAnime

    /**
     * Returns the Jsoup selector that returns the <a> tag linking to the next page, or null if
     * there's no next page.
     */
    protected abstract fun popularAnimeNextPageSelector(): String?

    /**
     * Parses the response from the site and returns a [AnimesPage] object.
     * Normally it's not needed to override this method.
     *
     * @param response the response from the site.
     */
    override fun searchAnimeParse(response: Response): AnimesPage {
        val document = response.asJsoup()

        val animes = document.select(this.searchAnimeSelector()).map { element ->
            this.searchAnimeFromElement(element)
        }

        val hasNextPage = this.searchAnimeNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return AnimesPage(animes, hasNextPage)
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each anime.
     */
    protected abstract fun searchAnimeSelector(): String

    /**
     * Returns a anime from the given [element]. Most sites only show the title and the url, it's
     * totally fine to fill only those two values.
     *
     * @param element an element obtained from [searchAnimeSelector].
     */
    protected abstract fun searchAnimeFromElement(element: Element): SAnime

    /**
     * Returns the Jsoup selector that returns the <a> tag linking to the next page, or null if
     * there's no next page.
     */
    protected abstract fun searchAnimeNextPageSelector(): String?

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
    override fun animeDetailsParse(response: Response): SAnime {
        return this.animeDetailsParse(response.asJsoup())
    }

    /**
     * Returns the details of the anime from the given [document].
     *
     * @param document the parsed document.
     */
    protected abstract fun animeDetailsParse(document: Document): SAnime

    // KMK -->
    /**
     * Parses the response from the site and returns a list of related animes.
     * Normally it's not needed to override this method.
     *
     * @since komikku/extensions-lib 1.6
     * @param response the response from the site.
     */
    override fun relatedAnimeListParse(response: Response): List<SAnime> {
        return response.asJsoup()
            .select(this.relatedAnimeListSelector()).map { this.relatedAnimeFromElement(it) }
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each related animes.
     *
     * @since komikku/extensions-lib 1.6
     */
    protected open fun relatedAnimeListSelector(): String = this.popularAnimeSelector()

    /**
     * Returns a anime from the given element.
     *
     * @since komikku/extensions-lib 1.6
     * @param element an element obtained from [relatedAnimeListSelector].
     */
    protected open fun relatedAnimeFromElement(element: Element): SAnime = this.popularAnimeFromElement(element)
    // KMK <--

    /**
     * Parses the response from the site and returns a list of episodes.
     * Normally it's not needed to override this method.
     *
     * @param response the response from the site.
     */
    override fun episodeListParse(response: Response): List<SEpisode> {
        val document = response.asJsoup()
        return document.select(this.episodeListSelector()).map { this.episodeFromElement(it) }
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each episode.
     */
    protected abstract fun episodeListSelector(): String

    /**
     * Returns a episode from the given element.
     *
     * @param element an element obtained from [episodeListSelector].
     */
    protected abstract fun episodeFromElement(element: Element): SEpisode

    /**
     * Parses the response from the site and returns the video list.
     * Normally it's not needed to override this method.
     *
     * @param response the response from the site.
     */
    override fun videoListParse(response: Response): List<Video> {
        return videoListParse(response.asJsoup())
    }

    /**
     * Returns a video list from the given document.
     *
     * @param document the parsed document.
     */
    protected abstract fun videoListParse(document: Document): List<Video>

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
