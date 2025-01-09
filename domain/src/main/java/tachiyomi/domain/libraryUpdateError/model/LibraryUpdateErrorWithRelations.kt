package tachiyomi.domain.libraryUpdateError.model

import tachiyomi.domain.anime.model.MangaCover

data class LibraryUpdateErrorWithRelations(
    val mangaId: Long,
    val mangaTitle: String,
    val mangaSource: Long,
    val mangaCover: MangaCover,
    val errorId: Long,
    val messageId: Long,
)
