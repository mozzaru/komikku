package eu.kanade.tachiyomi.data.track.myanimelist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MALAnime(
    val id: Long,
    val title: String,
    val synopsis: String = "",
    @SerialName("num_episodes")
    val numEpisodes: Long,
    val mean: Double = -1.0,
    @SerialName("main_picture")
    val covers: MALAnimeCovers,
    val status: String,
    @SerialName("media_type")
    val mediaType: String,
    @SerialName("start_date")
    val startDate: String?,
)

@Serializable
data class MALAnimeCovers(
    val large: String = "",
    val medium: String,
)

@Serializable
data class MALAnimeMetadata(
    val id: Long,
    val title: String,
    val synopsis: String?,
    @SerialName("main_picture")
    val covers: MALAnimeCovers,
    val authors: List<MALAuthor>,
)

@Serializable
data class MALAuthor(
    val node: MALAuthorNode,
    val role: String,
)

@Serializable
data class MALAuthorNode(
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
)
