package tachiyomi.domain.anime.model

data class MergedAnimeReference(
    // Tag identifier, unique
    val id: Long,

    // The anime where it grabs the updated anime info
    val isInfoAnime: Boolean,

    // If false the anime will not grab episode updates
    val getEpisodeUpdates: Boolean,

    // The mode in which the episodes are handeled, only set in the main merge reference
    val episodeSortMode: Int,

    // episode priority the deduplication uses
    val episodePriority: Int,

    // Set if you want it to download new episodes
    val downloadEpisodes: Boolean,

    // merged anime this reference is attached to
    val mergeId: Long?,

    // merged anime url this reference is attached to
    val mergeUrl: String,

    // anime id included in the merge this reference is attached to
    val animeId: Long?,

    // anime url included in the merge this reference is attached to
    val animeUrl: String,

    // source of the anime that is merged into this merge
    val animeSourceId: Long,
) {
    companion object {
        const val CHAPTER_SORT_NONE = 0
        const val CHAPTER_SORT_NO_DEDUPE = 1
        const val CHAPTER_SORT_PRIORITY = 2
        const val CHAPTER_SORT_MOST_CHAPTERS = 3
        const val CHAPTER_SORT_HIGHEST_CHAPTER_NUMBER = 4
    }
}
