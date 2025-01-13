package eu.kanade.tachiyomi.ui.reader.loader

import android.content.Context
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.all.MergedSource
import eu.kanade.tachiyomi.ui.reader.model.ReaderEpisode
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import mihon.core.archive.archiveReader
import mihon.core.archive.epubReader
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.MergedAnimeReference
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.io.Format

/**
 * Loader used to retrieve the [PageLoader] for a given episode.
 */
class EpisodeLoader(
    private val context: Context,
    private val downloadManager: DownloadManager,
    private val downloadProvider: DownloadProvider,
    private val anime: Anime,
    private val source: Source,
    // SY -->
    private val sourceManager: SourceManager,
    private val readerPrefs: ReaderPreferences,
    private val mergedReferences: List<MergedAnimeReference>,
    private val mergedAnime: Map<Long, Anime>,
    // SY <--
) {

    /**
     * Assigns the episode's page loader and loads the its pages. Returns immediately if the episode
     * is already loaded.
     */
    suspend fun loadEpisode(episode: ReaderEpisode /* SY --> */, page: Int? = null/* SY <-- */) {
        if (episodeIsReady(episode)) {
            return
        }

        episode.state = ReaderEpisode.State.Loading
        withIOContext {
            logcat { "Loading pages for ${episode.episode.name}" }
            try {
                val loader = getPageLoader(episode)
                episode.pageLoader = loader

                val pages = loader.getPages()
                    .onEach { it.episode = episode }

                if (pages.isEmpty()) {
                    throw Exception(context.stringResource(MR.strings.page_list_empty_error))
                }

                // If the episode is partially read, set the starting page to the last the user read
                // otherwise use the requested page.
                if (!episode.episode.read /* --> EH */ ||
                    readerPrefs
                        .preserveReadingPosition()
                        .get() ||
                    page != null // <-- EH
                ) {
                    episode.requestedPage = /* SY --> */ page ?: /* SY <-- */ episode.episode.last_page_read
                }

                episode.state = ReaderEpisode.State.Loaded(pages)
            } catch (e: Throwable) {
                episode.state = ReaderEpisode.State.Error(e)
                throw e
            }
        }
    }

    /**
     * Checks [episode] to be loaded based on present pages and loader in addition to state.
     */
    private fun episodeIsReady(episode: ReaderEpisode): Boolean {
        return episode.state is ReaderEpisode.State.Loaded && episode.pageLoader != null
    }

    /**
     * Returns the page loader to use for this [episode].
     */
    private fun getPageLoader(episode: ReaderEpisode): PageLoader {
        val dbEpisode = episode.episode
        val isDownloaded = downloadManager.isEpisodeDownloaded(
            episodeName = dbEpisode.name,
            episodeScanlator = dbEpisode.scanlator, /* SY --> */
            animeTitle = anime.ogTitle /* SY <-- */,
            sourceId = anime.source,
            skipCache = true,
        )
        return when {
            // SY -->
            source is MergedSource -> {
                val animeReference = mergedReferences.firstOrNull {
                    it.animeId == episode.episode.anime_id
                } ?: error("Merge reference null")
                val source = sourceManager.get(animeReference.animeSourceId)
                    ?: error("Source ${animeReference.animeSourceId} was null")
                val anime = mergedAnime[episode.episode.anime_id] ?: error("Anime for merged episode was null")
                val isMergedAnimeDownloaded = downloadManager.isEpisodeDownloaded(
                    episodeName = episode.episode.name,
                    episodeScanlator = episode.episode.scanlator,
                    animeTitle = anime.ogTitle,
                    sourceId = anime.source,
                    skipCache = true,
                )
                when {
                    isMergedAnimeDownloaded -> DownloadPageLoader(
                        episode = episode,
                        anime = anime,
                        source = source,
                        downloadManager = downloadManager,
                        downloadProvider = downloadProvider,
                    )
                    source is HttpSource -> HttpPageLoader(episode, source)
                    source is LocalSource -> source.getFormat(episode.episode).let { format ->
                        when (format) {
                            is Format.Directory -> DirectoryPageLoader(format.file)
                            is Format.Archive -> ArchivePageLoader(format.file.archiveReader(context))
                            is Format.Epub -> EpubPageLoader(format.file.epubReader(context))
                        }
                    }
                    else -> error(context.stringResource(MR.strings.loader_not_implemented_error))
                }
            }
            // SY <--
            isDownloaded -> DownloadPageLoader(
                episode,
                anime,
                source,
                downloadManager,
                downloadProvider,
            )
            source is LocalSource -> source.getFormat(episode.episode).let { format ->
                when (format) {
                    is Format.Directory -> DirectoryPageLoader(format.file)
                    is Format.Archive -> ArchivePageLoader(format.file.archiveReader(context))
                    is Format.Epub -> EpubPageLoader(format.file.epubReader(context))
                }
            }
            source is HttpSource -> HttpPageLoader(episode, source)
            source is StubSource -> error(context.stringResource(MR.strings.source_not_installed, source.toString()))
            else -> error(context.stringResource(MR.strings.loader_not_implemented_error))
        }
    }
}
