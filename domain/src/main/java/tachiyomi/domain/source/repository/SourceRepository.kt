package tachiyomi.domain.source.repository

import androidx.paging.PagingSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.animesource.model.SAnime
import exh.metadata.metadata.RaisedSearchMetadata
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.model.SourceWithCount

typealias SourcePagingSourceType = PagingSource<Long, /*SY --> */ Pair<SAnime, RaisedSearchMetadata?>/*SY <-- */>

interface SourceRepository {

    fun getSources(): Flow<List<Source>>

    fun getOnlineSources(): Flow<List<Source>>

    fun getSourcesWithFavoriteCount(): Flow<List<Pair<Source, Long>>>

    fun getSourcesWithNonLibraryManga(): Flow<List<SourceWithCount>>

    fun search(sourceId: Long, query: String, filterList: FilterList): SourcePagingSourceType

    fun getPopular(sourceId: Long): SourcePagingSourceType

    fun getLatest(sourceId: Long): SourcePagingSourceType
}
