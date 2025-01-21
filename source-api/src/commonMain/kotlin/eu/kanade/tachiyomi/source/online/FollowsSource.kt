package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import exh.metadata.metadata.RaisedSearchMetadata

interface FollowsSource : CatalogueSource {
    suspend fun fetchFollows(page: Int): MangasPage

    /**
     * Returns a list of all Follows retrieved by Coroutines
     *
     * @param SAnime all SAnime found for user
     */
    suspend fun fetchAllFollows(): List<Pair<SAnime, RaisedSearchMetadata>>
}
