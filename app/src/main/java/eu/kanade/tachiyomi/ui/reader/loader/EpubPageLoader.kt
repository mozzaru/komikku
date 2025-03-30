package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.source.model.Video
import eu.kanade.tachiyomi.ui.reader.model.ReaderVideo
import mihon.core.archive.EpubReader

/**
 * Loader used to load a episode from a .epub file.
 */
internal class EpubPageLoader(private val reader: EpubReader) : PageLoader() {

    override var isLocal: Boolean = true

    override suspend fun getPages(): List<ReaderVideo> {
        return reader.getImagesFromPages().mapIndexed { i, path ->
            ReaderVideo(i).apply {
                stream = { reader.getInputStream(path)!! }
                status = Video.State.READY
            }
        }
    }

    override suspend fun loadPage(page: ReaderVideo) {
        check(!isRecycled)
    }

    override fun recycle() {
        super.recycle()
        reader.close()
    }
}
