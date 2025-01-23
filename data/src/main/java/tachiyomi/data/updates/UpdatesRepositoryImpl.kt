package tachiyomi.data.updates

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.data.AndroidDatabaseHandler
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.anime.model.AnimeCover
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.domain.updates.repository.UpdatesRepository
import tachiyomi.view.UpdatesView

class UpdatesRepositoryImpl(
    private val databaseHandler: DatabaseHandler,
) : UpdatesRepository {

    override suspend fun awaitWithSeen(
        seen: Boolean,
        after: Long,
        limit: Long,
    ): List<UpdatesWithRelations> {
        return databaseHandler.awaitList {
            updatesViewQueries.getUpdatesBySeenStatus(
                read = seen,
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

    override fun subscribeWithSeen(
        seen: Boolean,
        after: Long,
        limit: Long,
    ): Flow<List<UpdatesWithRelations>> {
        return databaseHandler.subscribeToList {
            updatesViewQueries.getUpdatesBySeenStatus(
                read = seen,
                after = after,
                limit = limit,
                mapper = ::mapUpdatesWithRelations,
            )
        }
    }

    private fun mapUpdatesWithRelations(
        animeId: Long,
        animeTitle: String,
        episodeId: Long,
        episodeName: String,
        scanlator: String?,
        seen: Boolean,
        bookmark: Boolean,
        lastSecondSeen: Long,
        sourceId: Long,
        favorite: Boolean,
        thumbnailUrl: String?,
        coverLastModified: Long,
        dateUpload: Long,
        dateFetch: Long,
    ): UpdatesWithRelations = UpdatesWithRelations(
        animeId = animeId,
        // SY -->
        ogAnimeTitle = animeTitle,
        // SY <--
        episodeId = episodeId,
        episodeName = episodeName,
        scanlator = scanlator,
        seen = seen,
        bookmark = bookmark,
        lastSecondSeen = lastSecondSeen,
        sourceId = sourceId,
        dateFetch = dateFetch,
        coverData = AnimeCover(
            animeId = animeId,
            sourceId = sourceId,
            isAnimeFavorite = favorite,
            ogUrl = thumbnailUrl,
            lastModified = coverLastModified,
        ),
    )

    fun mapUpdatesView(updatesView: UpdatesView): UpdatesWithRelations {
        return UpdatesWithRelations(
            animeId = updatesView.mangaId,
            ogAnimeTitle = updatesView.mangaTitle,
            episodeId = updatesView.chapterId,
            episodeName = updatesView.chapterName,
            scanlator = updatesView.scanlator,
            seen = updatesView.read,
            bookmark = updatesView.bookmark,
            lastSecondSeen = updatesView.last_page_read,
            sourceId = updatesView.source,
            dateFetch = updatesView.datefetch,
            coverData = AnimeCover(
                animeId = updatesView.mangaId,
                sourceId = updatesView.source,
                isAnimeFavorite = updatesView.favorite,
                ogUrl = updatesView.thumbnailUrl,
                lastModified = updatesView.coverLastModified,
            ),
        )
    }
}
