package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.cache.EpisodeCache
import eu.kanade.tachiyomi.data.database.models.toDomainEpisode
import eu.kanade.tachiyomi.source.model.Video
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderVideo
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import exh.util.DataSaver
import exh.util.DataSaver.Companion.getImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

/**
 * Loader used to load episodes from an online source.
 */
@OptIn(DelicateCoroutinesApi::class)
internal class HttpPageLoader(
    private val chapter: ReaderChapter,
    private val source: HttpSource,
    private val episodeCache: EpisodeCache = Injekt.get(),
    // SY -->
    private val readerPreferences: ReaderPreferences = Injekt.get(),
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    // SY <--
) : PageLoader() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * A queue used to manage requests one by one while allowing priorities.
     */
    private val queue = PriorityBlockingQueue<PriorityPage>()

    private val preloadSize = /* SY --> */ readerPreferences.preloadSize().get() // SY <--

    // SY -->
    private val dataSaver = DataSaver(source, sourcePreferences)
    // SY <--

    init {
        // EXH -->
        repeat(readerPreferences.readerThreads().get()) {
            // EXH <--
            scope.launchIO {
                flow {
                    while (true) {
                        emit(runInterruptible { queue.take() }.page)
                    }
                }
                    .filter { it.status == Video.State.QUEUE }
                    .collect(::internalLoadPage)
            }
            // EXH -->
        }
        // EXH <--
    }

    override var isLocal: Boolean = false

    /**
     * Returns the page list for a episode. It tries to return the page list from the local cache,
     * otherwise fallbacks to network.
     */
    override suspend fun getPages(): List<ReaderVideo> {
        val pages = try {
            episodeCache.getPageListFromCache(chapter.episode.toDomainEpisode()!!)
        } catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }
            source.getVideoList(chapter.episode)
        }
        // SY -->
        val rp = pages.mapIndexed { index, page ->
            // Don't trust sources and use our own indexing
            ReaderVideo(index, page.url, page.videoUrl)
        }
        if (readerPreferences.aggressivePageLoading().get()) {
            rp.forEach {
                if (it.status == Video.State.QUEUE) {
                    queue.offer(PriorityPage(it, 0))
                }
            }
        }
        return rp
        // SY <--
    }

    /**
     * Loads a page through the queue. Handles re-enqueueing pages if they were evicted from the cache.
     */
    override suspend fun loadPage(page: ReaderVideo) = withIOContext {
        val imageUrl = page.videoUrl

        // Check if the image has been deleted
        if (page.status == Video.State.READY && imageUrl != null && !episodeCache.isImageInCache(imageUrl)) {
            page.status = Video.State.QUEUE
        }

        // Automatically retry failed pages when subscribed to this page
        if (page.status == Video.State.ERROR) {
            page.status = Video.State.QUEUE
        }

        val queuedPages = mutableListOf<PriorityPage>()
        if (page.status == Video.State.QUEUE) {
            queuedPages += PriorityPage(page, 1).also { queue.offer(it) }
        }
        queuedPages += preloadNextPages(page, preloadSize)

        suspendCancellableCoroutine<Nothing> { continuation ->
            continuation.invokeOnCancellation {
                queuedPages.forEach {
                    if (it.page.status == Video.State.QUEUE) {
                        queue.remove(it)
                    }
                }
            }
        }
    }

    /**
     * Retries a page. This method is only called from user interaction on the viewer.
     */
    override fun retryPage(page: ReaderVideo) {
        if (page.status == Video.State.ERROR) {
            page.status = Video.State.QUEUE
        }
        // EXH -->
        if (readerPreferences.readerInstantRetry().get()) // EXH <--
            {
                boostPage(page)
            } else {
            // EXH <--
            queue.offer(PriorityPage(page, 2))
        }
    }

    override fun recycle() {
        super.recycle()
        scope.cancel()
        queue.clear()

        // Cache current page list progress for online episodes to allow a faster reopen
        chapter.pages?.let { pages ->
            launchIO {
                try {
                    // Convert to pages without reader information
                    val pagesToSave = pages.map { Video(it.index, it.url, it.videoUrl) }
                    episodeCache.putPageListToCache(chapter.episode.toDomainEpisode()!!, pagesToSave)
                } catch (e: Throwable) {
                    if (e is CancellationException) {
                        throw e
                    }
                }
            }
        }
    }

    /**
     * Preloads the given [amount] of pages after the [currentPage] with a lower priority.
     *
     * @return a list of [PriorityPage] that were added to the [queue]
     */
    private fun preloadNextPages(currentPage: ReaderVideo, amount: Int): List<PriorityPage> {
        val pageIndex = currentPage.index
        val pages = currentPage.chapter.pages ?: return emptyList()
        if (pageIndex == pages.lastIndex) return emptyList()

        return pages
            .subList(pageIndex + 1, min(pageIndex + 1 + amount, pages.size))
            .mapNotNull {
                if (it.status == Video.State.QUEUE) {
                    PriorityPage(it, 0).apply { queue.offer(this) }
                } else {
                    null
                }
            }
    }

    /**
     * Loads the page, retrieving the image URL and downloading the image if necessary.
     * Downloaded images are stored in the episode cache.
     *
     * @param page the page whose source image has to be downloaded.
     */
    private suspend fun internalLoadPage(page: ReaderVideo) {
        try {
            if (page.videoUrl.isNullOrEmpty()) {
                page.status = Video.State.LOAD_PAGE
                page.videoUrl = source.getVideoUrl(page)
            }
            val imageUrl = page.videoUrl!!

            if (!episodeCache.isImageInCache(imageUrl)) {
                page.status = Video.State.DOWNLOAD_IMAGE
                val imageResponse = source.getImage(page, dataSaver)
                episodeCache.putImageToCache(imageUrl, imageResponse)
            }

            page.stream = { episodeCache.getImageFile(imageUrl).inputStream() }
            page.status = Video.State.READY
        } catch (e: Throwable) {
            page.status = Video.State.ERROR
            if (e is CancellationException) {
                throw e
            }
        }
    }

    // EXH -->
    fun boostPage(page: ReaderVideo) {
        if (page.status == Video.State.QUEUE) {
            scope.launchIO {
                loadPage(page)
            }
        }
    }
    // EXH <--
}

/**
 * Data class used to keep ordering of pages in order to maintain priority.
 */
private class PriorityPage(
    val page: ReaderVideo,
    val priority: Int,
) : Comparable<PriorityPage> {
    companion object {
        private val idGenerator = AtomicInteger()
    }

    private val identifier = idGenerator.incrementAndGet()

    override fun compareTo(other: PriorityPage): Int {
        val p = other.priority.compareTo(priority)
        return if (p != 0) p else identifier.compareTo(other.identifier)
    }
}
