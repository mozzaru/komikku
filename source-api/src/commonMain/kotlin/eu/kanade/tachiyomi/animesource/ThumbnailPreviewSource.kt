package eu.kanade.tachiyomi.animesource

import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.network.ProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import okhttp3.CacheControl
import okhttp3.Response

interface ThumbnailPreviewSource : AnimeSource {

    suspend fun getThumbnailPreviewList(anime: SAnime, episodes: List<SEpisode>, page: Int): ThumbnailPreviewImage

    suspend fun fetchPreviewImage(page: ThumbnailPreviewInfo, cacheControl: CacheControl? = null): Response
}

@Serializable
data class ThumbnailPreviewImage(
    val page: Int,
    val thumbnailPreviews: List<ThumbnailPreviewInfo>,
    val hasNextPage: Boolean,
    val pagePreviewPages: Int?,
)

@Serializable
data class ThumbnailPreviewInfo(
    val index: Int,
    val imageUrl: String,
    @Transient
    private val _progress: MutableStateFlow<Int> = MutableStateFlow(-1),
) : ProgressListener {
    @Transient
    val progress = _progress.asStateFlow()

    override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
        _progress.value = if (contentLength > 0) {
            (100 * bytesRead / contentLength).toInt()
        } else {
            -1
        }
    }
}
