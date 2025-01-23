package tachiyomi.data.source

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.MetadataAnimesPage
import eu.kanade.tachiyomi.source.model.SAnime
import exh.metadata.metadata.RaisedSearchMetadata

abstract class EHentaiPagingSource(override val source: CatalogueSource) : SourcePagingSource(source) {

    override fun getPageLoadResult(
        params: LoadParams<Long>,
        mangasPage: MangasPage,
    ): LoadResult.Page<Long, Pair<SAnime, RaisedSearchMetadata?>> {
        mangasPage as MetadataAnimesPage
        val metadata = mangasPage.animesMetadata

        return LoadResult.Page(
            data = mangasPage.animes
                .mapIndexed { index, sManga -> sManga to metadata.getOrNull(index) },
            prevKey = null,
            nextKey = mangasPage.nextKey,
        )
    }
}

class EHentaiSearchPagingSource(
    source: CatalogueSource,
    val query: String,
    val filters: FilterList,
) : EHentaiPagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): MangasPage {
        return source.getSearchManga(currentPage, query, filters)
    }
}

class EHentaiPopularPagingSource(source: CatalogueSource) : EHentaiPagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): MangasPage {
        return source.getPopularManga(currentPage)
    }
}

class EHentaiLatestPagingSource(source: CatalogueSource) : EHentaiPagingSource(source) {
    override suspend fun requestNextPage(currentPage: Int): MangasPage {
        return source.getLatestUpdates(currentPage)
    }
}
