package tachiyomi.data.source

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.MetadataAnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import exh.metadata.metadata.RaisedSearchMetadata

abstract class EHentaiPagingSource(override val source: AnimeCatalogueSource) : SourcePagingSource(source) {

    override fun getPageLoadResult(
        params: LoadParams<Long>,
        animesPage: AnimesPage,
    ): LoadResult.Page<Long, Pair<SAnime, RaisedSearchMetadata?>> {
        animesPage as MetadataAnimesPage
        val metadata = animesPage.mangasMetadata

        return LoadResult.Page(
            data = animesPage.animes
                .mapIndexed { index, sManga -> sManga to metadata.getOrNull(index) },
            prevKey = null,
            nextKey = animesPage.nextKey,
        )
    }
}

class EHentaiSearchPagingSource(
    source: AnimeCatalogueSource,
    val query: String,
    val filters: AnimeFilterList,
) : EHentaiPagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): AnimesPage {
        return source.getSearchAnime(currentPage, query, filters)
    }
}

class EHentaiPopularPagingSource(source: AnimeCatalogueSource) : EHentaiPagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): AnimesPage {
        return source.getPopularAnime(currentPage)
    }
}

class EHentaiLatestPagingSource(source: AnimeCatalogueSource) : EHentaiPagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): AnimesPage {
        return source.getLatestUpdates(currentPage)
    }
}
