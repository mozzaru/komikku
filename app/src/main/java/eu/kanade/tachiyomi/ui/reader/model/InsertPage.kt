package eu.kanade.tachiyomi.ui.reader.model

class InsertPage(val parent: ReaderPage) : ReaderPage(parent.index, parent.url, parent.imageUrl) {

    override var episode: ReaderEpisode = parent.episode

    init {
        status = State.READY
        stream = parent.stream
    }
}
