package tachiyomi.domain.manga.model

data class MergeAnimeSettingsUpdate(
    val id: Long,
    var isInfoAnime: Boolean?,
    var getEpisodeUpdates: Boolean?,
    var episodePriority: Int?,
    var downloadEpisodes: Boolean?,
    var episodeSortMode: Int?,
)
