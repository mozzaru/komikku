package eu.kanade.tachiyomi.animesource.online

import eu.kanade.tachiyomi.animesource.Source

interface RandomMangaSource : Source {
    suspend fun fetchRandomMangaUrl(): String
}
