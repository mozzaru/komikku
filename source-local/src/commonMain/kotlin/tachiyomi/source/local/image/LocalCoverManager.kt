package tachiyomi.source.local.image

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.SAnime
import java.io.InputStream

expect class LocalCoverManager {

    fun find(animeUrl: String): UniFile?

    // SY -->
    fun update(anime: SAnime, inputStream: InputStream, encrypted: Boolean = false): UniFile?
    // SY <--
}
