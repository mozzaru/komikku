package eu.kanade.tachiyomi.ui.reader.model

data class ViewerEpisodes(
    val currEpisode: ReaderEpisode,
    val prevEpisode: ReaderEpisode?,
    val nextEpisode: ReaderEpisode?,
) {

    fun ref() {
        currEpisode.ref()
        prevEpisode?.ref()
        nextEpisode?.ref()
    }

    fun unref() {
        currEpisode.unref()
        prevEpisode?.unref()
        nextEpisode?.unref()
    }
}
