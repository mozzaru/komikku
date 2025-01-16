package tachiyomi.data.libraryUpdateError

import tachiyomi.domain.anime.model.AnimeCover
import tachiyomi.domain.libraryUpdateError.model.LibraryUpdateErrorWithRelations

val libraryUpdateErrorWithRelationsMapper:
    (Long, String, Long, Boolean, String?, Long, Long, Long) -> LibraryUpdateErrorWithRelations =
    { mangaId, mangaTitle, mangaSource, favorite, mangaThumbnail, coverLastModified, errorId, messageId ->
        LibraryUpdateErrorWithRelations(
            mangaId = mangaId,
            mangaTitle = mangaTitle,
            mangaSource = mangaSource,
            animeCover = AnimeCover(
                animeId = mangaId,
                sourceId = mangaSource,
                isAnimeFavorite = favorite,
                ogUrl = mangaThumbnail,
                lastModified = coverLastModified,
            ),
            errorId = errorId,
            messageId = messageId,
        )
    }
