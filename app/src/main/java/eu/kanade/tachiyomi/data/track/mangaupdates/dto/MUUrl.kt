package eu.kanade.tachiyomi.data.track.animeupdates.dto

import kotlinx.serialization.Serializable

@Serializable
data class MUUrl(
    val original: String? = null,
    val thumb: String? = null,
)
