package eu.kanade.tachiyomi.animesource.online

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import exh.metadata.metadata.RaisedSearchMetadata

interface FollowsSource : AnimeCatalogueSource {
    suspend fun fetchFollows(page: Int): AnimesPage

    /**
     * Returns a list of all Follows retrieved by Coroutines
     *
     * @param SManga all smanga found for user
     */
    suspend fun fetchAllFollows(): List<Pair<SAnime, RaisedSearchMetadata>>
}
