package exh.md.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AnimeListDto(
    override val limit: Int,
    override val offset: Int,
    override val total: Int,
    override val data: List<AnimeDataDto>,
) : ListCallDto<AnimeDataDto>

@Serializable
data class AnimeDto(
    val result: String,
    val data: AnimeDataDto,
)

@Serializable
data class AnimeDataDto(
    val id: String,
    val type: String,
    val attributes: AnimeAttributesDto,
    val relationships: List<RelationshipDto>,
)

@Serializable
data class AnimeAttributesDto(
    val title: JsonElement,
    val altTitles: List<Map<String, String>>,
    val description: JsonElement,
    val links: JsonElement?,
    val originalLanguage: String,
    val lastVolume: String?,
    val lastEpisode: String?,
    val contentRating: String?,
    val publicationDemographic: String?,
    val status: String?,
    val year: Int?,
    val tags: List<TagDto>,
)

@Serializable
data class TagDto(
    val id: String,
    val attributes: TagAttributesDto,
)

@Serializable
data class TagAttributesDto(
    val name: Map<String, String>,
)

@Serializable
data class RelationshipDto(
    val id: String,
    val type: String,
    val attributes: IncludesAttributesDto? = null,
)

@Serializable
data class IncludesAttributesDto(
    val name: String? = null,
    val fileName: String? = null,
)

@Serializable
data class AuthorListDto(
    val results: List<AuthorDto>,
)

@Serializable
data class AuthorDto(
    val result: String,
    val data: AuthorDataDto,
)

@Serializable
data class AuthorDataDto(
    val id: String,
    val attributes: AuthorAttributesDto,
)

@Serializable
data class AuthorAttributesDto(
    val name: String,
)

@Serializable
data class ReadingStatusDto(
    val status: String?,
)

@Serializable
data class ReadingStatusMapDto(
    val statuses: Map<String, String?>,
)

@Serializable
data class ReadEpisodeDto(
    val data: List<String>,
)

@Serializable
data class CoverListDto(
    val data: List<CoverDto>,
)

@Serializable
data class CoverDto(
    val id: String,
    val attributes: CoverAttributesDto,
    val relationships: List<RelationshipDto>,
)

@Serializable
data class CoverAttributesDto(
    val fileName: String,
)

@Serializable
data class AggregateDto(
    val result: String,
    val volumes: Map<String, AggregateVolume>,
)

@Serializable
data class AggregateVolume(
    val volume: String,
    val count: String,
    val episodes: Map<String, AggregateEpisode>,
)

@Serializable
data class AggregateEpisode(
    val episode: String,
    val count: String,
)
