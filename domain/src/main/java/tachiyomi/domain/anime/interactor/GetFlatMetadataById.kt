package tachiyomi.domain.anime.interactor

import eu.kanade.tachiyomi.source.online.MetadataSource
import exh.metadata.metadata.base.FlatMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.anime.repository.AnimeMetadataRepository

class GetFlatMetadataById(
    private val animeMetadataRepository: AnimeMetadataRepository,
) : MetadataSource.GetFlatMetadataById {

    override suspend fun await(id: Long): FlatMetadata? {
        return try {
            val meta = animeMetadataRepository.getMetadataById(id)
            return if (meta != null) {
                val tags = animeMetadataRepository.getTagsById(id)
                val titles = animeMetadataRepository.getTitlesById(id)

                FlatMetadata(meta, tags, titles)
            } else {
                null
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            null
        }
    }

    fun subscribe(id: Long): Flow<FlatMetadata?> {
        return combine(
            animeMetadataRepository.subscribeMetadataById(id),
            animeMetadataRepository.subscribeTagsById(id),
            animeMetadataRepository.subscribeTitlesById(id),
        ) { meta, tags, titles ->
            if (meta != null) {
                FlatMetadata(meta, tags, titles)
            } else {
                null
            }
        }
    }
}
