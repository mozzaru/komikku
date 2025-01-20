package exh.source

import eu.kanade.tachiyomi.animesource.AnimeSource
import tachiyomi.domain.anime.model.Anime

// Used to speed up isLewdSource
var metadataDelegatedSourceIds: List<Long> = emptyList()

var nHentaiSourceIds: List<Long> = emptyList()

var mangaDexSourceIds: List<Long> = emptyList()

var LIBRARY_UPDATE_EXCLUDED_SOURCES = listOf(
    EH_SOURCE_ID,
    EXH_SOURCE_ID,
    PURURIN_SOURCE_ID,
)

// This method MUST be fast!
fun isMetadataSource(source: Long) = source in 6900..6999 ||
    metadataDelegatedSourceIds.binarySearch(source) >= 0

fun AnimeSource.isEhBasedSource() = id == EH_SOURCE_ID || id == EXH_SOURCE_ID

fun AnimeSource.isMdBasedSource() = id in mangaDexSourceIds

fun Anime.isEhBasedManga() = source == EH_SOURCE_ID || source == EXH_SOURCE_ID

fun AnimeSource.getMainSource(): AnimeSource = if (this is EnhancedAnimeHttpSource) {
    this.source()
} else {
    this
}

@JvmName("getMainSourceInline")
inline fun <reified T : AnimeSource> AnimeSource.getMainSource(): T? = if (this is EnhancedAnimeHttpSource) {
    this.source() as? T
} else {
    this as? T
}

fun AnimeSource.getOriginalSource(): AnimeSource = if (this is EnhancedAnimeHttpSource) {
    this.originalSource
} else {
    this
}

fun AnimeSource.getEnhancedSource(): AnimeSource = if (this is EnhancedAnimeHttpSource) {
    this.enhancedSource
} else {
    this
}

inline fun <reified T> AnimeSource.anyIs(): Boolean {
    return if (this is EnhancedAnimeHttpSource) {
        originalSource is T || enhancedSource is T
    } else {
        this is T
    }
}
