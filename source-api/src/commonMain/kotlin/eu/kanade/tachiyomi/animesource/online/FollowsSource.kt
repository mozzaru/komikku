package eu.kanade.tachiyomi.animesource.online

import eu.kanade.tachiyomi.animesource.CatalogueSource
import eu.kanade.tachiyomi.animesource.model.MangasPage
import eu.kanade.tachiyomi.animesource.model.SManga
import exh.metadata.metadata.RaisedSearchMetadata

interface FollowsSource : CatalogueSource {
    suspend fun fetchFollows(page: Int): MangasPage

    /**
     * Returns a list of all Follows retrieved by Coroutines
     *
     * @param SManga all smanga found for user
     */
    suspend fun fetchAllFollows(): List<Pair<SManga, RaisedSearchMetadata>>
}
