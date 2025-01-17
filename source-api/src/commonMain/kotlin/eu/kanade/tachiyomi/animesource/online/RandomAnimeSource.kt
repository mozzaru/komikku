package eu.kanade.tachiyomi.animesource.online

import eu.kanade.tachiyomi.animesource.AnimeSource

interface RandomAnimeSource : AnimeSource {
    suspend fun fetchRandomMangaUrl(): String
}
