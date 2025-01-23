package exh.md.follows

import eu.kanade.tachiyomi.source.model.AnimesPage
import eu.kanade.tachiyomi.source.online.all.MangaDex
import tachiyomi.data.source.SourcePagingSource

/**
 * LatestUpdatesPager inherited from the general Pager.
 */
class MangaDexFollowsPagingSource(val mangadex: MangaDex) : SourcePagingSource(mangadex) {

    override suspend fun requestNextPage(currentPage: Int): AnimesPage {
        return mangadex.fetchFollows(currentPage)
    }
}
