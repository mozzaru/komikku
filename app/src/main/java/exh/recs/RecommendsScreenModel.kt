package exh.recs

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel
import kotlinx.coroutines.runBlocking
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.source.repository.SourcePagingSourceType
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class RecommendsScreenModel(
    val mangaId: Long,
    sourceId: Long,
    private val getAnime: GetAnime = Injekt.get(),
) : BrowseSourceScreenModel(sourceId, null) {

    val manga = runBlocking { getAnime.await(mangaId) }!!

    override fun createSourcePagingSource(query: String, filters: AnimeFilterList): SourcePagingSourceType {
        return RecommendsPagingSource(source as AnimeCatalogueSource, manga)
    }
}
