package eu.kanade.domain.anime.interactor

import eu.kanade.domain.anime.model.hasCustomCover
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import tachiyomi.domain.anime.interactor.FetchInterval
import tachiyomi.domain.anime.model.AnimeUpdate
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.repository.AnimeRepository
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.ZonedDateTime

class UpdateAnime(
    private val animeRepository: AnimeRepository,
    private val fetchInterval: FetchInterval,
) {

    suspend fun await(animeUpdate: AnimeUpdate): Boolean {
        return animeRepository.update(animeUpdate)
    }

    suspend fun awaitAll(animeUpdates: List<AnimeUpdate>): Boolean {
        return animeRepository.updateAll(animeUpdates)
    }

    suspend fun awaitUpdateFromSource(
        localAnime: Anime,
        remoteManga: SAnime,
        manualFetch: Boolean,
        coverCache: CoverCache = Injekt.get(),
        // SY -->
        downloadManager: DownloadManager = Injekt.get(),
        // SY <--
    ): Boolean {
        val remoteTitle = try {
            remoteManga.title
        } catch (_: UninitializedPropertyAccessException) {
            ""
        }

        // SY -->
        val title = if (remoteTitle.isNotBlank() && localAnime.ogTitle != remoteTitle) {
            downloadManager.renameMangaDir(localAnime.ogTitle, remoteTitle, localAnime.source)
            remoteTitle
        } else {
            null
        }
        // SY <--

        val coverLastModified =
            when {
                // Never refresh covers if the url is empty to avoid "losing" existing covers
                remoteManga.thumbnail_url.isNullOrEmpty() -> null
                !manualFetch && localAnime.thumbnailUrl == remoteManga.thumbnail_url -> null
                localAnime.isLocal() -> Instant.now().toEpochMilli()
                localAnime.hasCustomCover(coverCache) -> {
                    coverCache.deleteFromCache(localAnime, false)
                    null
                }
                else -> {
                    coverCache.deleteFromCache(localAnime, false)
                    Instant.now().toEpochMilli()
                }
            }

        val thumbnailUrl = remoteManga.thumbnail_url?.takeIf { it.isNotEmpty() }

        return animeRepository.update(
            AnimeUpdate(
                id = localAnime.id,
                title = title,
                coverLastModified = coverLastModified,
                author = remoteManga.author,
                artist = remoteManga.artist,
                description = remoteManga.description,
                genre = remoteManga.getGenres(),
                thumbnailUrl = thumbnailUrl,
                status = remoteManga.status.toLong(),
                updateStrategy = remoteManga.update_strategy,
                initialized = true,
            ),
        )
    }

    suspend fun awaitUpdateFetchInterval(
        anime: Anime,
        dateTime: ZonedDateTime = ZonedDateTime.now(),
        window: Pair<Long, Long> = fetchInterval.getWindow(dateTime),
    ): Boolean {
        return animeRepository.update(
            fetchInterval.toMangaUpdate(anime, dateTime, window),
        )
    }

    suspend fun awaitUpdateLastUpdate(mangaId: Long): Boolean {
        return animeRepository.update(AnimeUpdate(id = mangaId, lastUpdate = Instant.now().toEpochMilli()))
    }

    suspend fun awaitUpdateCoverLastModified(mangaId: Long): Boolean {
        return animeRepository.update(AnimeUpdate(id = mangaId, coverLastModified = Instant.now().toEpochMilli()))
    }

    suspend fun awaitUpdateFavorite(mangaId: Long, favorite: Boolean): Boolean {
        val dateAdded = when (favorite) {
            true -> Instant.now().toEpochMilli()
            false -> 0
        }
        return animeRepository.update(
            AnimeUpdate(id = mangaId, favorite = favorite, dateAdded = dateAdded),
        )
    }
}
