package tachiyomi.domain.history.model

import java.util.Date

data class History(
    val id: Long,
    val chapterId: Long,
    val seenAt: Date?,
    val readDuration: Long,
) {
    companion object {
        fun create() = History(
            id = -1L,
            chapterId = -1L,
            seenAt = null,
            readDuration = -1L,
        )
    }
}
