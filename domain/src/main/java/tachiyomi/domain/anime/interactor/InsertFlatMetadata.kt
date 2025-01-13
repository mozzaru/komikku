package tachiyomi.domain.anime.interactor

import eu.kanade.tachiyomi.source.online.MetadataSource
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.base.FlatMetadata
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.anime.repository.AnimeMetadataRepository

class InsertFlatMetadata(
    private val animeMetadataRepository: AnimeMetadataRepository,
) : MetadataSource.InsertFlatMetadata {

    suspend fun await(flatMetadata: FlatMetadata) {
        try {
            animeMetadataRepository.insertFlatMetadata(flatMetadata)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    override suspend fun await(metadata: RaisedSearchMetadata) {
        try {
            animeMetadataRepository.insertMetadata(metadata)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }
}
