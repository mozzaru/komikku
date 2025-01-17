package tachiyomi.domain.source.service

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.source.model.StubSource

interface SourceManager {

    val catalogueSources: Flow<List<AnimeCatalogueSource>>

    fun get(sourceKey: Long): AnimeSource?

    fun getOrStub(sourceKey: Long): AnimeSource

    fun getOnlineSources(): List<AnimeHttpSource>

    fun getCatalogueSources(): List<AnimeCatalogueSource>

    // SY -->
    val isInitialized: StateFlow<Boolean>

    fun getVisibleOnlineSources(): List<AnimeHttpSource>

    fun getVisibleCatalogueSources(): List<AnimeCatalogueSource>
    // SY <--

    fun getStubSources(): List<StubSource>
}
