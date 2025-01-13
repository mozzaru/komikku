package tachiyomi.domain.anime.repository

interface CustomAnimeRepository {

    fun get(animeId: Long): tachiyomi.domain.anime.model.CustomAnimeInfo?

    fun set(animeInfo: tachiyomi.domain.anime.model.CustomAnimeInfo)
}
