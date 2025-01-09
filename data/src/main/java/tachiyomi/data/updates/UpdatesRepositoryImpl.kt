package tachiyomi.data.updates

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.data.AndroidDatabaseHandler
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.anime.model.MangaCover
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.domain.updates.repository.UpdatesRepository
import tachiyomi.view.UpdatesView

class UpdatesRepositoryImpl(
    private val databaseHandler: DatabaseHandler,
) : UpdatesRepository {

    override suspend fun awaitWithRead(
        read: Boolean,
        after: Long,
        limit: Long,
    ): List<UpdatesWithRelations> {
        return databaseHandler.awaitList {
            updatesViewQueries.getUpdatesByReadStatus(
                read = read,
                after = after,
                limit = limit,
                mapper = ::mapUpdatesWithRelations,
            )
        }
    }

    override fun subscribeAll(after: Long, limit: Long): Flow<List<UpdatesWithRelations>> {
        return databaseHandler.subscribeToList {
            updatesViewQueries.getRecentUpdates(after, limit, ::mapUpdatesWithRelations)
        }.map {
            databaseHandler.awaitListExecutable {
                (databaseHandler as AndroidDatabaseHandler).getUpdatesQuery(after, limit)
            }
                .map(::mapUpdatesView)
        }
    }

    override fun subscribeWithRead(
        read: Boolean,
        after: Long,
        limit: Long,
    ): Flow<List<UpdatesWithRelations>> {
        return databaseHandler.subscribeToList {
            updatesViewQueries.getUpdatesByReadStatus(
                read = read,
                after = after,
                limit = limit,
                mapper = ::mapUpdatesWithRelations,
            )
        }
    }

    private fun mapUpdatesWithRelations(
        mangaId: Long,
        mangaTitle: String,
        chapterId: Long,
        chapterName: String,
        scanlator: String?,
        read: Boolean,
        bookmark: Boolean,
        lastPageRead: Long,
        sourceId: Long,
        favorite: Boolean,
        thumbnailUrl: String?,
        coverLastModified: Long,
        dateUpload: Long,
        dateFetch: Long,
    ): UpdatesWithRelations = UpdatesWithRelations(
        animeId = mangaId,
        // SY -->
        ogMangaTitle = mangaTitle,
        // SY <--
        episodeId = chapterId,
        chapterName = chapterName,
        scanlator = scanlator,
        read = read,
        bookmark = bookmark,
        lastPageRead = lastPageRead,
        sourceId = sourceId,
        dateFetch = dateFetch,
        coverData = MangaCover(
            animeId = mangaId,
            sourceId = sourceId,
            isAnimeFavorite = favorite,
            ogUrl = thumbnailUrl,
            lastModified = coverLastModified,
        ),
    )

    fun mapUpdatesView(updatesView: UpdatesView): UpdatesWithRelations {
        return UpdatesWithRelations(
            animeId = updatesView.mangaId,
            ogMangaTitle = updatesView.mangaTitle,
            episodeId = updatesView.chapterId,
            chapterName = updatesView.chapterName,
            scanlator = updatesView.scanlator,
            read = updatesView.read,
            bookmark = updatesView.bookmark,
            lastPageRead = updatesView.last_page_read,
            sourceId = updatesView.source,
            dateFetch = updatesView.datefetch,
            coverData = MangaCover(
                animeId = updatesView.mangaId,
                sourceId = updatesView.source,
                isAnimeFavorite = updatesView.favorite,
                ogUrl = updatesView.thumbnailUrl,
                lastModified = updatesView.coverLastModified,
            ),
        )
    }
}
