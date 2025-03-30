package eu.kanade.tachiyomi.ui.reader.loader

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.Video
import eu.kanade.tachiyomi.ui.reader.model.ReaderVideo
import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder
import tachiyomi.core.common.util.system.ImageUtil

/**
 * Loader used to load a episode from a directory given on [file].
 */
internal class DirectoryPageLoader(val file: UniFile) : PageLoader() {

    override var isLocal: Boolean = true

    override suspend fun getPages(): List<ReaderVideo> {
        return file.listFiles()
            ?.filter { !it.isDirectory && ImageUtil.isImage(it.name) { it.openInputStream() } }
            ?.sortedWith { f1, f2 -> f1.name.orEmpty().compareToCaseInsensitiveNaturalOrder(f2.name.orEmpty()) }
            ?.mapIndexed { i, file ->
                val streamFn = { file.openInputStream() }
                ReaderVideo(i).apply {
                    stream = streamFn
                    status = Video.State.READY
                }
            }
            .orEmpty()
    }
}
