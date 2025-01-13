package exh.recs

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel
import kotlinx.coroutines.runBlocking
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.source.repository.SourcePagingSourceType
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class RecommendsScreenModel(
    val animeId: Long,
    sourceId: Long,
    private val getAnime: GetAnime = Injekt.get(),
) : BrowseSourceScreenModel(sourceId, null) {

    val anime = runBlocking { getAnime.await(animeId) }!!

    override fun createSourcePagingSource(query: String, filters: FilterList): SourcePagingSourceType {
        return RecommendsPagingSource(source as CatalogueSource, anime)
    }
}
