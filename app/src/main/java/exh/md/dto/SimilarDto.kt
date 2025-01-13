package exh.md.dto

import kotlinx.serialization.Serializable

@Serializable
data class SimilarAnimeDto(
    val id: String,
    val title: Map<String, String>,
    val contentRating: String,
    val matches: List<SimilarAnimeMatchListDto>,
    val updatedAt: String,
)

@Serializable
data class SimilarAnimeMatchListDto(
    val id: String,
    val title: Map<String, String>,
    val contentRating: String,
    val score: Double,
)

@Serializable
data class RelationListDto(
    val response: String,
    val data: List<RelationDto>,
)

@Serializable
data class RelationDto(
    val attributes: RelationAttributesDto,
    val relationships: List<RelationAnimeDto>,
)

@Serializable
data class RelationAnimeDto(
    val id: String,
)

@Serializable
data class RelationAttributesDto(
    val relation: String,
)
