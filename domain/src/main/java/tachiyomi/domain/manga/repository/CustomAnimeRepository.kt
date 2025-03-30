package tachiyomi.domain.manga.repository

import tachiyomi.domain.manga.model.CustomAnimeInfo

interface CustomAnimeRepository {

    fun get(animeId: Long): CustomAnimeInfo?

    fun set(animeInfo: CustomAnimeInfo)
}
