package eu.kanade.tachiyomi.source.online.all

import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.domain.anime.model.toSAnime
import eu.kanade.domain.episode.interactor.SyncEpisodesWithSource
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.copy
import eu.kanade.tachiyomi.source.online.HttpSource
import exh.source.MERGED_SOURCE_ID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mihon.domain.chapter.interactor.FilterChaptersForDownload
import okhttp3.Response
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.GetAnime
import tachiyomi.domain.manga.interactor.GetMergedReferencesById
import tachiyomi.domain.manga.interactor.NetworkToLocalAnime
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.injectLazy

class MergedSource : HttpSource() {
    private val getAnime: GetAnime by injectLazy()
    private val getMergedReferencesById: GetMergedReferencesById by injectLazy()
    private val syncEpisodesWithSource: SyncEpisodesWithSource by injectLazy()
    private val networkToLocalAnime: NetworkToLocalAnime by injectLazy()
    private val updateAnime: UpdateAnime by injectLazy()
    private val sourceManager: SourceManager by injectLazy()
    private val downloadManager: DownloadManager by injectLazy()
    private val filterChaptersForDownload: FilterChaptersForDownload by injectLazy()

    override val id: Long = MERGED_SOURCE_ID

    override val baseUrl = ""

    override fun popularMangaRequest(page: Int) = throw UnsupportedOperationException()
    override fun popularMangaParse(response: Response) = throw UnsupportedOperationException()
    override fun searchMangaRequest(
        page: Int,
        query: String,
        filters: FilterList,
    ) = throw UnsupportedOperationException()
    override fun searchMangaParse(response: Response) = throw UnsupportedOperationException()
    override fun latestUpdatesRequest(page: Int) = throw UnsupportedOperationException()
    override fun latestUpdatesParse(response: Response) = throw UnsupportedOperationException()
    override fun mangaDetailsParse(response: Response) = throw UnsupportedOperationException()
    override fun chapterListParse(response: Response) = throw UnsupportedOperationException()
    override fun chapterPageParse(response: Response) = throw UnsupportedOperationException()
    override fun pageListParse(response: Response) = throw UnsupportedOperationException()
    override fun pageUrlParse(response: Response) = throw UnsupportedOperationException()

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getEpisodeList(anime)"))
    override fun fetchChapterList(manga: SManga) = throw UnsupportedOperationException()
    override suspend fun getChapterList(manga: SManga) = throw UnsupportedOperationException()
    override suspend fun getImage(page: Page): Response = throw UnsupportedOperationException()

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getImageUrl(page)"))
    override fun fetchImageUrl(page: Page) = throw UnsupportedOperationException()
    override suspend fun getImageUrl(page: Page) = throw UnsupportedOperationException()

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getPageList(chapter)"))
    override fun fetchPageList(chapter: SChapter) = throw UnsupportedOperationException()
    override suspend fun getPageList(chapter: SChapter) = throw UnsupportedOperationException()

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getLatestUpdates(page)"))
    override fun fetchLatestUpdates(page: Int) = throw UnsupportedOperationException()
    override suspend fun getLatestUpdates(page: Int) = throw UnsupportedOperationException()

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getPopularAnime(page)"))
    override fun fetchPopularManga(page: Int) = throw UnsupportedOperationException()
    override suspend fun getPopularManga(page: Int) = throw UnsupportedOperationException()

    override suspend fun getMangaDetails(manga: SManga): SManga {
        return withIOContext {
            val mergedAnime = requireNotNull(getAnime.await(manga.url, id)) { "merged anime not in db" }
            val animeReferences = getMergedReferencesById.await(mergedAnime.id)
                .apply {
                    require(isNotEmpty()) { "Anime references are empty, info unavailable, merge is likely corrupted" }
                    require(!(size == 1 && first().mangaSourceId == MERGED_SOURCE_ID)) {
                        "Anime references contain only the merged reference, merge is likely corrupted"
                    }
                }

            val animeInfoReference = animeReferences.firstOrNull { it.isInfoManga }
                ?: animeReferences.firstOrNull { it.mangaId != it.mergeId }
            val dbAnime = animeInfoReference?.run {
                getAnime.await(mangaUrl, mangaSourceId)?.toSAnime()
            }
            (dbAnime ?: mergedAnime.toSAnime()).copy(
                url = manga.url,
            )
        }
    }

    suspend fun fetchEpisodesForMergedAnime(
        manga: Manga,
        downloadEpisodes: Boolean = true,
    ) {
        fetchEpisodesAndSync(manga, downloadEpisodes)
    }

    suspend fun fetchEpisodesAndSync(manga: Manga, downloadEpisodes: Boolean = true): List<Chapter> {
        val animeReferences = getMergedReferencesById.await(manga.id)
        require(animeReferences.isNotEmpty()) {
            "Anime references are empty, episodes unavailable, merge is likely corrupted"
        }

        val semaphore = Semaphore(5)
        var exception: Exception? = null
        return supervisorScope {
            animeReferences
                .groupBy(MergedMangaReference::mangaSourceId)
                .minus(MERGED_SOURCE_ID)
                .map { (_, values) ->
                    async {
                        semaphore.withPermit {
                            values.flatMap {
                                try {
                                    val (source, loadedAnime, reference) = it.load()
                                    if (loadedAnime != null && reference.getChapterUpdates) {
                                        val episodeList = source.getChapterList(loadedAnime.toSAnime())
                                        val results =
                                            syncEpisodesWithSource.await(episodeList, loadedAnime, source)

                                        if (downloadEpisodes && reference.downloadChapters) {
                                            val episodesToDownload = filterChaptersForDownload.await(manga, results)
                                            if (episodesToDownload.isNotEmpty()) {
                                                downloadManager.downloadEpisodes(
                                                    loadedAnime,
                                                    episodesToDownload,
                                                )
                                            }
                                        }
                                        results
                                    } else {
                                        emptyList()
                                    }
                                } catch (e: Exception) {
                                    if (e is CancellationException) throw e
                                    exception = e
                                    emptyList()
                                }
                            }
                        }
                    }
                }
                .awaitAll()
                .flatten()
        }.also {
            exception?.let { throw it }
        }
    }

    suspend fun MergedMangaReference.load(): LoadedAnimeSource {
        var anime = getAnime.await(mangaUrl, mangaSourceId)
        val source = sourceManager.getOrStub(anime?.source ?: mangaSourceId)
        if (anime == null) {
            val newManga = networkToLocalAnime.await(
                Manga.create().copy(
                    source = mangaSourceId,
                    url = mangaUrl,
                ),
            )
            updateAnime.awaitUpdateFromSource(newManga, source.getMangaDetails(newManga.toSAnime()), false)
            anime = getAnime.await(newManga.id)!!
        }
        return LoadedAnimeSource(source, anime, this)
    }

    data class LoadedAnimeSource(val source: Source, val manga: Manga?, val reference: MergedMangaReference)

    override val lang = "all"
    override val supportsLatest = false
    override val name = "MergedSource"
}
