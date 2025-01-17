package eu.kanade.tachiyomi.source.online.all

import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.domain.anime.model.toSManga
import eu.kanade.domain.episode.interactor.SyncEpisodesWithSource
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.Page
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.copy
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import exh.source.MERGED_SOURCE_ID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mihon.domain.episode.interactor.FilterEpisodesForDownload
import okhttp3.Response
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.anime.interactor.GetMergedReferencesById
import tachiyomi.domain.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.anime.model.MergedAnimeReference
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.injectLazy

class MergedSource : AnimeHttpSource() {
    private val getAnime: GetAnime by injectLazy()
    private val getMergedReferencesById: GetMergedReferencesById by injectLazy()
    private val syncEpisodesWithSource: SyncEpisodesWithSource by injectLazy()
    private val networkToLocalAnime: NetworkToLocalAnime by injectLazy()
    private val updateAnime: UpdateAnime by injectLazy()
    private val sourceManager: SourceManager by injectLazy()
    private val downloadManager: DownloadManager by injectLazy()
    private val filterEpisodesForDownload: FilterEpisodesForDownload by injectLazy()

    override val id: Long = MERGED_SOURCE_ID

    override val baseUrl = ""

    override fun popularAnimeRequest(page: Int) = throw UnsupportedOperationException()
    override fun popularAnimeParse(response: Response) = throw UnsupportedOperationException()
    override fun searchAnimeRequest(
        page: Int,
        query: String,
        filters: AnimeFilterList,
    ) = throw UnsupportedOperationException()
    override fun searchAnimeParse(response: Response) = throw UnsupportedOperationException()
    override fun latestUpdatesRequest(page: Int) = throw UnsupportedOperationException()
    override fun latestUpdatesParse(response: Response) = throw UnsupportedOperationException()
    override fun animeDetailsParse(response: Response) = throw UnsupportedOperationException()
    override fun episodeListParse(response: Response) = throw UnsupportedOperationException()
    override fun episodeVideoParse(response: Response) = throw UnsupportedOperationException()
    override fun videoListParse(response: Response) = throw UnsupportedOperationException()
    override fun videoUrlParse(response: Response) = throw UnsupportedOperationException()

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getEpisodeList"))
    override fun fetchEpisodeList(anime: SAnime) = throw UnsupportedOperationException()
    override suspend fun getEpisodeList(manga: SAnime) = throw UnsupportedOperationException()
    override suspend fun getImage(video: Page): Response = throw UnsupportedOperationException()

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getImageUrl"))
    override fun fetchImageUrl(video: Page) = throw UnsupportedOperationException()
    override suspend fun getImageUrl(page: Page) = throw UnsupportedOperationException()

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getVideoList"))
    override fun fetchVideoList(episode: SEpisode) = throw UnsupportedOperationException()
    override suspend fun getVideoList(chapter: SEpisode) = throw UnsupportedOperationException()

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getLatestUpdates"))
    override fun fetchLatestUpdates(page: Int) = throw UnsupportedOperationException()
    override suspend fun getLatestUpdates(page: Int) = throw UnsupportedOperationException()

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getPopularAnime"))
    override fun fetchPopularAnime(page: Int) = throw UnsupportedOperationException()
    override suspend fun getPopularAnime(page: Int) = throw UnsupportedOperationException()

    override suspend fun getAnimeDetails(anime: SAnime): SAnime {
        return withIOContext {
            val mergedManga = requireNotNull(getAnime.await(anime.url, id)) { "merged manga not in db" }
            val mangaReferences = getMergedReferencesById.await(mergedManga.id)
                .apply {
                    require(isNotEmpty()) { "Manga references are empty, info unavailable, merge is likely corrupted" }
                    require(!(size == 1 && first().mangaSourceId == MERGED_SOURCE_ID)) {
                        "Manga references contain only the merged reference, merge is likely corrupted"
                    }
                }

            val mangaInfoReference = mangaReferences.firstOrNull { it.isInfoManga }
                ?: mangaReferences.firstOrNull { it.mangaId != it.mergeId }
            val dbManga = mangaInfoReference?.run {
                getAnime.await(mangaUrl, mangaSourceId)?.toSManga()
            }
            (dbManga ?: mergedManga.toSManga()).copy(
                url = anime.url,
            )
        }
    }

    suspend fun fetchChaptersForMergedManga(
        manga: Manga,
        downloadChapters: Boolean = true,
    ) {
        fetchChaptersAndSync(manga, downloadChapters)
    }

    suspend fun fetchChaptersAndSync(manga: Manga, downloadChapters: Boolean = true): List<Episode> {
        val mangaReferences = getMergedReferencesById.await(manga.id)
        require(mangaReferences.isNotEmpty()) {
            "Manga references are empty, episodes unavailable, merge is likely corrupted"
        }

        val semaphore = Semaphore(5)
        var exception: Exception? = null
        return supervisorScope {
            mangaReferences
                .groupBy(MergedAnimeReference::mangaSourceId)
                .minus(MERGED_SOURCE_ID)
                .map { (_, values) ->
                    async {
                        semaphore.withPermit {
                            values.flatMap {
                                try {
                                    val (source, loadedManga, reference) = it.load()
                                    if (loadedManga != null && reference.getChapterUpdates) {
                                        val chapterList = source.getEpisodeList(loadedManga.toSManga())
                                        val results =
                                            syncEpisodesWithSource.await(chapterList, loadedManga, source)

                                        if (downloadChapters && reference.downloadChapters) {
                                            val chaptersToDownload = filterEpisodesForDownload.await(manga, results)
                                            if (chaptersToDownload.isNotEmpty()) {
                                                downloadManager.downloadChapters(
                                                    loadedManga,
                                                    chaptersToDownload,
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

    suspend fun MergedAnimeReference.load(): LoadedMangaSource {
        var manga = getAnime.await(mangaUrl, mangaSourceId)
        val source = sourceManager.getOrStub(manga?.source ?: mangaSourceId)
        if (manga == null) {
            val newManga = networkToLocalAnime.await(
                Manga.create().copy(
                    source = mangaSourceId,
                    url = mangaUrl,
                ),
            )
            updateAnime.awaitUpdateFromSource(newManga, source.getAnimeDetails(newManga.toSManga()), false)
            manga = getAnime.await(newManga.id)!!
        }
        return LoadedMangaSource(source, manga, this)
    }

    data class LoadedMangaSource(val source: AnimeSource, val manga: Manga?, val reference: MergedAnimeReference)

    override val lang = "all"
    override val supportsLatest = false
    override val name = "MergedSource"
}
