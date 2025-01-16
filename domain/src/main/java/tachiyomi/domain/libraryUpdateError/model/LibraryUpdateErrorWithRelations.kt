package tachiyomi.domain.libraryUpdateError.model

import tachiyomi.domain.anime.model.AnimeCover

data class LibraryUpdateErrorWithRelations(
    val mangaId: Long,
    val mangaTitle: String,
    val mangaSource: Long,
    val animeCover: AnimeCover,
    val errorId: Long,
    val messageId: Long,
)
