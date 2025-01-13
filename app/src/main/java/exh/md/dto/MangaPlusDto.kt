package exh.md.dto

import kotlinx.serialization.Serializable

@Serializable
data class AnimePlusResponse(
    val success: SuccessResult? = null,
)

@Serializable
data class SuccessResult(
    val animeViewer: AnimeViewer? = null,
)

@Serializable
data class AnimeViewer(val pages: List<AnimePlusPage> = emptyList())

@Serializable
data class AnimePlusPage(val animePage: AnimePage? = null)

@Serializable
data class AnimePage(
    val imageUrl: String,
    val width: Int,
    val height: Int,
    val encryptionKey: String? = null,
)
