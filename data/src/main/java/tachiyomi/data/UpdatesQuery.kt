package tachiyomi.data

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import exh.source.MERGED_SOURCE_ID
import tachiyomi.view.UpdatesView

private val mapper = { cursor: SqlCursor ->
    UpdatesView(
        cursor.getLong(0)!!,
        cursor.getString(1)!!,
        cursor.getLong(2)!!,
        cursor.getString(3)!!,
        cursor.getString(4),
        cursor.getLong(5)!! == 1L,
        cursor.getLong(6)!! == 1L,
        cursor.getLong(7)!!,
        cursor.getLong(8)!!,
        cursor.getLong(9)!! == 1L,
        cursor.getString(10),
        cursor.getLong(11)!!,
        cursor.getLong(12)!!,
        cursor.getLong(13)!!,
    )
}

class UpdatesQuery(val driver: SqlDriver, val after: Long, val limit: Long) : ExecutableQuery<UpdatesView>(mapper) {
    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
        return driver.executeQuery(
            null,
            """
            SELECT
                animes._id AS animeId,
                animes.title AS animeTitle,
                episodes._id AS episodeId,
                episodes.name AS episodeName,
                episodes.scanlator,
                episodes.read,
                episodes.bookmark,
                episodes.last_page_read,
                animes.source,
                animes.favorite,
                animes.thumbnail_url AS thumbnailUrl,
                animes.cover_last_modified AS coverLastModified,
                episodes.date_upload AS dateUpload,
                episodes.date_fetch AS datefetch
            FROM animes JOIN episodes
            ON animes._id = episodes.anime_id
            WHERE favorite = 1 AND source <> $MERGED_SOURCE_ID
            AND date_fetch > date_added
            AND dateUpload > :after
            UNION
            SELECT
                animes._id AS animeId,
                animes.title AS animeTitle,
                episodes._id AS episodeId,
                episodes.name AS episodeName,
                episodes.scanlator,
                episodes.read,
                episodes.bookmark,
                episodes.last_page_read,
                animes.source,
                animes.favorite,
                animes.thumbnail_url AS thumbnailUrl,
                animes.cover_last_modified AS coverLastModified,
                episodes.date_upload AS dateUpload,
                episodes.date_fetch AS datefetch
            FROM animes
            LEFT JOIN (
                SELECT merged.anime_id,merged.merge_id
                FROM merged
                GROUP BY merged.merge_id
            ) as ME
            ON ME.merge_id = animes._id
            JOIN episodes
            ON ME.anime_id = episodes.anime_id
            WHERE favorite = 1 AND source = $MERGED_SOURCE_ID
            AND date_fetch > date_added
            AND dateUpload > :after
            ORDER BY datefetch DESC
            LIMIT :limit;
            """.trimIndent(),
            mapper,
            2,
            binders = {
                bindLong(0, after)
                bindLong(1, limit)
            },
        )
    }

    override fun toString(): String = "LibraryQuery.sq:get"
}
