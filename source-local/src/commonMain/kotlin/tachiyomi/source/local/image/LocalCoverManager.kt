package tachiyomi.source.local.image

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.SAnime
import java.io.InputStream

expect class LocalCoverManager {

    fun find(mangaUrl: String): UniFile?

    // SY -->
    fun update(manga: SAnime, inputStream: InputStream, encrypted: Boolean = false): UniFile?
    // SY <--
}
