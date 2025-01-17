package tachiyomi.data.source

import androidx.paging.PagingState
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.MetadataAnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import exh.metadata.metadata.RaisedSearchMetadata
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.source.repository.SourcePagingSourceType

class SourceSearchPagingSource(source: AnimeCatalogueSource, val query: String, val filters: AnimeFilterList) :
    SourcePagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): AnimesPage {
        return source.getSearchAnime(currentPage, query, filters)
    }
}

class SourcePopularPagingSource(source: AnimeCatalogueSource) : SourcePagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): AnimesPage {
        return source.getPopularAnime(currentPage)
    }
}

class SourceLatestPagingSource(source: AnimeCatalogueSource) : SourcePagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): AnimesPage {
        return source.getLatestUpdates(currentPage)
    }
}

abstract class SourcePagingSource(
    protected open val source: AnimeCatalogueSource,
) : SourcePagingSourceType() {

    abstract suspend fun requestNextPage(currentPage: Int): AnimesPage

    override suspend fun load(
        params: LoadParams<Long>,
    ): LoadResult<Long, /*SY --> */ Pair<SAnime, RaisedSearchMetadata?>/*SY <-- */> {
        val page = params.key ?: 1

        val mangasPage = try {
            withIOContext {
                requestNextPage(page.toInt())
                    .takeIf { it.animes.isNotEmpty() }
                    ?: throw NoResultsException()
            }
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }

        // SY -->
        return getPageLoadResult(params, mangasPage)
        // SY <--
    }

    // SY -->
    open fun getPageLoadResult(
        params: LoadParams<Long>,
        animesPage: AnimesPage,
    ): LoadResult.Page<Long, /*SY --> */ Pair<SAnime, RaisedSearchMetadata?>/*SY <-- */> {
        val page = params.key ?: 1

        // SY -->
        val metadata = if (animesPage is MetadataAnimesPage) {
            animesPage.mangasMetadata
        } else {
            emptyList()
        }
        // SY <--

        return LoadResult.Page(
            data = animesPage.animes
                // SY -->
                .mapIndexed { index, sManga -> sManga to metadata.getOrNull(index) },
            // SY <--
            prevKey = null,
            nextKey = if (animesPage.hasNextPage) page + 1 else null,
        )
    }
    // SY <--

    override fun getRefreshKey(
        state: PagingState<Long, /*SY --> */ Pair<SAnime, RaisedSearchMetadata?>/*SY <-- */>,
    ): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }
}

class NoResultsException : Exception()
