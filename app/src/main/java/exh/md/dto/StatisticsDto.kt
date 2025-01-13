package exh.md.dto

import kotlinx.serialization.Serializable

@Serializable
data class StatisticsDto(
    val statistics: Map<String, StatisticsAnimeDto>,
)

@Serializable
data class StatisticsAnimeDto(
    val rating: StatisticsAnimeRatingDto,
)

@Serializable
data class StatisticsAnimeRatingDto(
    val average: Double?,
    val bayesian: Double?,
)
