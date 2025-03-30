package eu.kanade.tachiyomi.ui.reader.loader

import android.app.Application
import android.net.Uri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.database.models.toDomainEpisode
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.Video
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderVideo
import mihon.core.archive.archiveReader
import tachiyomi.domain.anime.model.Anime
import uy.kohesive.injekt.injectLazy

/**
 * Loader used to load a episode from the downloaded episodes.
 */
internal class DownloadPageLoader(
    private val chapter: ReaderChapter,
    private val anime: Anime,
    private val source: Source,
    private val downloadManager: DownloadManager,
    private val downloadProvider: DownloadProvider,
) : PageLoader() {

    private val context: Application by injectLazy()

    private var archivePageLoader: ArchivePageLoader? = null

    override var isLocal: Boolean = true

    override suspend fun getPages(): List<ReaderVideo> {
        val dbChapter = chapter.episode
        val chapterPath = downloadProvider.findEpisodeDir(dbChapter.name, dbChapter.scanlator, /* SY --> */ anime.ogTitle /* SY <-- */, source)
        return if (chapterPath?.isFile == true) {
            getPagesFromArchive(chapterPath)
        } else {
            getPagesFromDirectory()
        }
    }

    override fun recycle() {
        super.recycle()
        archivePageLoader?.recycle()
    }

    private suspend fun getPagesFromArchive(file: UniFile): List<ReaderVideo> {
        val loader = ArchivePageLoader(file.archiveReader(context)).also { archivePageLoader = it }
        return loader.getPages()
    }

    private fun getPagesFromDirectory(): List<ReaderVideo> {
        val pages = downloadManager.buildPageList(source, anime, chapter.episode.toDomainEpisode()!!)
        return pages.map { page ->
            ReaderVideo(page.index, page.url, page.videoUrl) {
                context.contentResolver.openInputStream(page.uri ?: Uri.EMPTY)!!
            }.apply {
                status = Video.State.READY
            }
        }
    }

    override suspend fun loadPage(page: ReaderVideo) {
        archivePageLoader?.loadPage(page)
    }
}
