package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.AnimesPage
import eu.kanade.tachiyomi.source.model.SAnime
import exh.metadata.metadata.RaisedSearchMetadata

interface FollowsSource : CatalogueSource {
    suspend fun fetchFollows(page: Int): AnimesPage

    /**
     * Returns a list of all Follows retrieved by Coroutines
     *
     * @param SAnime all SAnime found for user
     */
    suspend fun fetchAllFollows(): List<Pair<SAnime, RaisedSearchMetadata>>
}
