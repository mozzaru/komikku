package tachiyomi.domain.anime.model

data class MergeAnimeSettingsUpdate(
    val id: Long,
    var isInfoManga: Boolean?,
    var getChapterUpdates: Boolean?,
    var chapterPriority: Int?,
    var downloadChapters: Boolean?,
    var chapterSortMode: Int?,
)
