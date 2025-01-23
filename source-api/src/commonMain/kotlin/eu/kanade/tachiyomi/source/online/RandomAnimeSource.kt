package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.Source

interface RandomAnimeSource : Source {
    suspend fun fetchRandomAnimeUrl(): String
}
