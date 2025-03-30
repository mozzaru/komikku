package eu.kanade.tachiyomi.ui.reader.model

class InsertVideo(val parent: ReaderVideo) : ReaderVideo(parent.index, parent.url, parent.videoUrl) {

    override var chapter: ReaderChapter = parent.chapter

    init {
        status = State.READY
        stream = parent.stream
    }
}
