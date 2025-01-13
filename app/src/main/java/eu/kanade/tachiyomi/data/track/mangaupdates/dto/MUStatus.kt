package eu.kanade.tachiyomi.data.track.animeupdates.dto

import kotlinx.serialization.Serializable

@Serializable
data class MUStatus(
    val volume: Int? = null,
    val episode: Int? = null,
)
