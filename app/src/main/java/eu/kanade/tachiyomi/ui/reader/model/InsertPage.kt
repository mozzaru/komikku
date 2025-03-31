package eu.kanade.tachiyomi.ui.reader.model

class InsertPage(val parent: ReaderPage) : ReaderPage(parent.index, parent.url, parent.pageUrl) {

    override var chapter: ReaderChapter = parent.chapter

    init {
        status = State.READY
        stream = parent.stream
    }
}
