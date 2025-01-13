package eu.kanade.tachiyomi.data.track.animeupdates.dto

import kotlinx.serialization.Serializable

@Serializable
data class MUSeries(
    val id: Long? = null,
    val title: String? = null,
)
