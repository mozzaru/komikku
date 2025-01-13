package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.domain.anime.model.MergedAnimeReference

/*
* SY merged anime backup class
 */
@Serializable
data class BackupMergedAnimeReference(
    @ProtoNumber(1) var isInfoAnime: Boolean,
    @ProtoNumber(2) var getEpisodeUpdates: Boolean,
    @ProtoNumber(3) var episodeSortMode: Int,
    @ProtoNumber(4) var episodePriority: Int,
    @ProtoNumber(5) var downloadEpisodes: Boolean,
    @ProtoNumber(6) var mergeUrl: String,
    @ProtoNumber(7) var animeUrl: String,
    @ProtoNumber(8) var animeSourceId: Long,
) {
    fun getMergedAnimeReference(): MergedAnimeReference {
        return MergedAnimeReference(
            isInfoAnime = isInfoAnime,
            getEpisodeUpdates = getEpisodeUpdates,
            episodeSortMode = episodeSortMode,
            episodePriority = episodePriority,
            downloadEpisodes = downloadEpisodes,
            mergeUrl = mergeUrl,
            animeUrl = animeUrl,
            animeSourceId = animeSourceId,
            mergeId = null,
            animeId = null,
            id = -1,
        )
    }
}

val backupMergedAnimeReferenceMapper =
    {
            _: Long,
            isInfoAnime: Boolean,
            getEpisodeUpdates: Boolean,
            episodeSortMode: Long,
            episodePriority: Long,
            downloadEpisodes: Boolean,
            _: Long,
            mergeUrl: String,
            _: Long?,
            animeUrl: String,
            animeSourceId: Long,
        ->
        BackupMergedAnimeReference(
            isInfoAnime = isInfoAnime,
            getEpisodeUpdates = getEpisodeUpdates,
            episodeSortMode = episodeSortMode.toInt(),
            episodePriority = episodePriority.toInt(),
            downloadEpisodes = downloadEpisodes,
            mergeUrl = mergeUrl,
            animeUrl = animeUrl,
            animeSourceId = animeSourceId,
        )
    }
