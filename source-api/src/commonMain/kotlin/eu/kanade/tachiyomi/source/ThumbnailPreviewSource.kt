package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.network.ProgressListener
import eu.kanade.tachiyomi.source.model.SEpisode
import eu.kanade.tachiyomi.source.model.SAnime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import okhttp3.CacheControl
import okhttp3.Response

interface ThumbnailPreviewSource : Source {

    suspend fun getThumbnailPreviewList(anime: SAnime, episodes: List<SEpisode>, page: Int): ThumbnailPreviewPage

    suspend fun fetchPreviewImage(thumbnail: ThumbnailPreviewInfo, cacheControl: CacheControl? = null): Response
}

@Serializable
data class ThumbnailPreviewPage(
    val page: Int,
    val pagePreviews: List<ThumbnailPreviewInfo>,
    val hasNextPage: Boolean,
    val thumbnailPreviewPages: Int?,
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
