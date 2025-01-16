package tachiyomi.domain.anime.interactor

import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.anime.repository.AnimeRepository

class NetworkToLocalAnime(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(manga: Manga): Manga {
        val localManga = getManga(manga.url, manga.source)
        return when {
            localManga == null -> {
                val id = insertManga(manga)
                manga.copy(id = id!!)
            }
            !localManga.favorite -> {
                // if the manga isn't a favorite, set its display title from source
                // if it later becomes a favorite, updated title will go to db
                localManga.copy(/* SY --> */ogTitle/* SY <-- */ = manga.title)
            }
            else -> {
                localManga
            }
        }
    }

    // KMK -->
    suspend fun getLocal(manga: Manga): Manga = if (manga.id <= 0) {
        await(manga)
    } else {
        manga
    }
    // KMK <--

    private suspend fun getManga(url: String, sourceId: Long): Manga? {
        return animeRepository.getMangaByUrlAndSourceId(url, sourceId)
    }

    private suspend fun insertManga(manga: Manga): Long? {
        return animeRepository.insert(manga)
    }
}
