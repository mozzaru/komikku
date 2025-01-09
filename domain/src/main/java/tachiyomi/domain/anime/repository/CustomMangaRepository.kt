package tachiyomi.domain.anime.repository

import tachiyomi.domain.anime.model.CustomMangaInfo

interface CustomMangaRepository {

    fun get(mangaId: Long): CustomMangaInfo?

    fun set(mangaInfo: CustomMangaInfo)
}
