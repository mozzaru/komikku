package tachiyomi.domain.source.service

import eu.kanade.tachiyomi.animesource.CatalogueSource
import eu.kanade.tachiyomi.animesource.Source
import eu.kanade.tachiyomi.animesource.online.HttpSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.source.model.StubSource

interface SourceManager {

    val catalogueSources: Flow<List<CatalogueSource>>

    fun get(sourceKey: Long): Source?

    fun getOrStub(sourceKey: Long): Source

    fun getOnlineSources(): List<HttpSource>

    fun getCatalogueSources(): List<CatalogueSource>

    // SY -->
    val isInitialized: StateFlow<Boolean>

    fun getVisibleOnlineSources(): List<HttpSource>

    fun getVisibleCatalogueSources(): List<CatalogueSource>
    // SY <--

    fun getStubSources(): List<StubSource>
}
