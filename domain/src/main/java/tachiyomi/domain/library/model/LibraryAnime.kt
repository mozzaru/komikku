package tachiyomi.domain.library.model

import tachiyomi.domain.anime.model.Anime

data class LibraryAnime(
    val anime: Anime,
    val category: Long,
    val totalChapters: Long,
    val readCount: Long,
    val bookmarkCount: Long,
    val latestUpload: Long,
    val chapterFetchedAt: Long,
    val lastRead: Long,
) {
    val id: Long = anime.id

    val unreadCount
        get() = totalChapters - readCount

    val hasBookmarks
        get() = bookmarkCount > 0

    val hasStarted = readCount > 0
}
