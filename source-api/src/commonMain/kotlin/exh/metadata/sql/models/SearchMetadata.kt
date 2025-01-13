package exh.metadata.sql.models

import kotlinx.serialization.Serializable

@Serializable
data class SearchMetadata(
    // Anime ID this gallery is linked to
    val animeId: Long,

    // Gallery uploader
    val uploader: String?,

    // Extra data attached to this metadata, in JSON format
    val extra: String,

    // Indexed extra data attached to this metadata
    val indexedExtra: String?,

    // The version of this metadata's extra. Used to track changes to the 'extra' field's schema
    val extraVersion: Int,
)
