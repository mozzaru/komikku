package exh.md.dto

import kotlinx.serialization.Serializable

@Serializable
data class EpisodeListDto(
    override val limit: Int,
    override val offset: Int,
    override val total: Int,
    override val data: List<EpisodeDataDto>,
) : ListCallDto<EpisodeDataDto>

@Serializable
data class EpisodeDto(
    val result: String,
    val data: EpisodeDataDto,
)

@Serializable
data class EpisodeDataDto(
    val id: String,
    val type: String,
    val attributes: EpisodeAttributesDto,
    val relationships: List<RelationshipDto>,
)

@Serializable
data class EpisodeAttributesDto(
    val title: String?,
    val volume: String?,
    val episode: String?,
    val translatedLanguage: String,
    val externalUrl: String?,
    val pages: Int,
    val version: Int,
    val createdAt: String,
    val updatedAt: String,
    val publishAt: String,
    val readableAt: String,
)

@Serializable
data class GroupListDto(
    override val limit: Int,
    override val offset: Int,
    override val total: Int,
    override val data: List<GroupDataDto>,
) : ListCallDto<GroupDataDto>

@Serializable
data class GroupDto(
    val result: String,
    val data: GroupDataDto,
)

@Serializable
data class GroupDataDto(
    val id: String,
    val attributes: GroupAttributesDto,
)

@Serializable
data class GroupAttributesDto(
    val name: String,
)
