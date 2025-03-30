package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SChapter
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
     * Parses the response from the site and returns a [MangasPage] object.
     * Normally it's not needed to override this method.
     *
     * @param response the response from the site.
     */
    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val animes = document.select(this.popularMangaSelector()).map { element ->
            this.popularMangaFromElement(element)
        }

        val hasNextPage = this.popularMangaNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return MangasPage(animes, hasNextPage)
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each anime.
     */
    protected abstract fun popularMangaSelector(): String

    /**
     * Returns a anime from the given [element]. Most sites only show the title and the url, it's
     * totally fine to fill only those two values.
     *
     * @param element an element obtained from [popularMangaSelector].
     */
    protected abstract fun popularMangaFromElement(element: Element): SManga

    /**
     * Returns the Jsoup selector that returns the <a> tag linking to the next page, or null if
     * there's no next page.
     */
    protected abstract fun popularMangaNextPageSelector(): String?

    /**
     * Parses the response from the site and returns a [MangasPage] object.
     * Normally it's not needed to override this method.
     *
     * @param response the response from the site.
     */
    override fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val animes = document.select(this.searchMangaSelector()).map { element ->
            this.searchMangaFromElement(element)
        }

        val hasNextPage = this.searchMangaNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return MangasPage(animes, hasNextPage)
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each anime.
     */
    protected abstract fun searchMangaSelector(): String

    /**
     * Returns a anime from the given [element]. Most sites only show the title and the url, it's
     * totally fine to fill only those two values.
     *
     * @param element an element obtained from [searchMangaSelector].
     */
    protected abstract fun searchMangaFromElement(element: Element): SManga

    /**
     * Returns the Jsoup selector that returns the <a> tag linking to the next page, or null if
     * there's no next page.
     */
    protected abstract fun searchMangaNextPageSelector(): String?

    /**
     * Parses the response from the site and returns a [MangasPage] object.
     * Normally it's not needed to override this method.
     *
     * @param response the response from the site.
     */
    override fun latestUpdatesParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val animes = document.select(latestUpdatesSelector()).map { element ->
            latestUpdatesFromElement(element)
        }

        val hasNextPage = latestUpdatesNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return MangasPage(animes, hasNextPage)
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
    protected abstract fun latestUpdatesFromElement(element: Element): SManga

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
    override fun mangaDetailsParse(response: Response): SManga {
        return this.mangaDetailsParse(response.asJsoup())
    }

    /**
     * Returns the details of the anime from the given [document].
     *
     * @param document the parsed document.
     */
    protected abstract fun mangaDetailsParse(document: Document): SManga

    // KMK -->
    /**
     * Parses the response from the site and returns a list of related animes.
     * Normally it's not needed to override this method.
     *
     * @since komikku/extensions-lib 1.6
     * @param response the response from the site.
     */
    override fun relatedMangaListParse(response: Response): List<SManga> {
        return response.asJsoup()
            .select(this.relatedMangaListSelector()).map { this.relatedMangaFromElement(it) }
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each related animes.
     *
     * @since komikku/extensions-lib 1.6
     */
    protected open fun relatedMangaListSelector(): String = this.popularMangaSelector()

    /**
     * Returns a anime from the given element.
     *
     * @since komikku/extensions-lib 1.6
     * @param element an element obtained from [relatedMangaListSelector].
     */
    protected open fun relatedMangaFromElement(element: Element): SManga = this.popularMangaFromElement(element)
    // KMK <--

    /**
     * Parses the response from the site and returns a list of episodes.
     * Normally it's not needed to override this method.
     *
     * @param response the response from the site.
     */
    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        return document.select(this.chapterListSelector()).map { this.chapterFromElement(it) }
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each episode.
     */
    protected abstract fun chapterListSelector(): String

    /**
     * Returns a episode from the given element.
     *
     * @param element an element obtained from [chapterListSelector].
     */
    protected abstract fun chapterFromElement(element: Element): SChapter

    /**
     * Parses the response from the site and returns the video list.
     * Normally it's not needed to override this method.
     *
     * @param response the response from the site.
     */
    override fun pageListParse(response: Response): List<Page> {
        return this.pageListParse(response.asJsoup())
    }

    /**
     * Returns a video list from the given document.
     *
     * @param document the parsed document.
     */
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
