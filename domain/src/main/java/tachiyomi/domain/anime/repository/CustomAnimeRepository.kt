package tachiyomi.domain.anime.repository

import tachiyomi.domain.anime.model.CustomAnimeInfo

interface CustomAnimeRepository {

    fun get(mangaId: Long): CustomAnimeInfo?

    fun set(mangaInfo: CustomAnimeInfo)
}
