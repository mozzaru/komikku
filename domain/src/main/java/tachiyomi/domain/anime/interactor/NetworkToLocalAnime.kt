package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.repository.AnimeRepository

class NetworkToLocalAnime(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(anime: Anime): Anime {
        val localManga = getManga(anime.url, anime.source)
        return when {
            localManga == null -> {
                val id = insertManga(anime)
                anime.copy(id = id!!)
            }
            !localManga.favorite -> {
                // if the anime isn't a favorite, set its display title from source
                // if it later becomes a favorite, updated title will go to db
                localManga.copy(/* SY --> */ogTitle/* SY <-- */ = anime.title)
            }
            else -> {
                localManga
            }
        }
    }

    // KMK -->
    suspend fun getLocal(anime: Anime): Anime = if (anime.id <= 0) {
        await(anime)
    } else {
        anime
    }
    // KMK <--

    private suspend fun getManga(url: String, sourceId: Long): Anime? {
        return animeRepository.getMangaByUrlAndSourceId(url, sourceId)
    }

    private suspend fun insertManga(anime: Anime): Long? {
        return animeRepository.insert(anime)
    }
}
