package exh.md.similar

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.online.all.MangaDex
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel
import exh.metadata.metadata.RaisedSearchMetadata
import exh.source.getMainSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.source.repository.SourcePagingSourceType
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaDexSimilarScreenModel(
    val animeId: Long,
    sourceId: Long,
    private val getAnime: GetAnime = Injekt.get(),
) : BrowseSourceScreenModel(sourceId, null) {

    val anime: Anime = runBlocking { getAnime.await(animeId) }!!

    override fun createSourcePagingSource(query: String, filters: FilterList): SourcePagingSourceType {
        return MangaDexSimilarPagingSource(anime, source.getMainSource() as MangaDex)
    }

    override fun Flow<Anime>.combineMetadata(metadata: RaisedSearchMetadata?): Flow<Pair<Anime, RaisedSearchMetadata?>> {
        return map { it to metadata }
    }

    init {
        mutableState.update { it.copy(filterable = false) }
    }
}
