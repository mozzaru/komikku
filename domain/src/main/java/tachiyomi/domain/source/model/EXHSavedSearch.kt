package tachiyomi.domain.source.model

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList

data class EXHSavedSearch(
    val id: Long,
    val name: String,
    val query: String?,
    val filterList: AnimeFilterList?,
)
