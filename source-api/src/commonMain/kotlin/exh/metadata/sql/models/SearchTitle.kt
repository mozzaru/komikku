package exh.metadata.sql.models

import kotlinx.serialization.Serializable

@Serializable
data class SearchTitle(
    // Title identifier, unique
    val id: Long?,

    // Metadata this title is attached to
    val animeId: Long,

    // Title
    val title: String,

    // Title type, useful for distinguishing between main/alt titles
    val type: Int,
)
