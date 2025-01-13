package exh.eh

import android.content.Context
import eu.kanade.domain.anime.interactor.UpdateAnime
import exh.metadata.metadata.EHentaiSearchMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.anime.interactor.InsertFavoriteEntryAlternative
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.AnimeUpdate
import tachiyomi.domain.anime.model.FavoriteEntryAlternative
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetAnimeCategories
import tachiyomi.domain.episode.interactor.GetEpisodeByUrl
import tachiyomi.domain.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.episode.model.EpisodeUpdate
import tachiyomi.domain.episode.repository.EpisodeRepository
import tachiyomi.domain.history.interactor.GetHistoryByAnimeId
import tachiyomi.domain.history.interactor.RemoveHistory
import tachiyomi.domain.history.interactor.UpsertHistory
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryUpdate
import uy.kohesive.injekt.injectLazy
import java.io.File

data class EpisodeChain(val anime: Anime, val episodes: List<Episode>, val history: List<History>)

class EHentaiUpdateHelper(context: Context) {
    val parentLookupTable =
        MemAutoFlushingLookupTable(
            File(context.filesDir, "exh-plt.maftable"),
            GalleryEntry.Serializer(),
        )
    private val getEpisodeByUrl: GetEpisodeByUrl by injectLazy()
    private val getEpisodesByAnimeId: GetEpisodesByAnimeId by injectLazy()
    private val getAnime: GetAnime by injectLazy()
    private val updateAnime: UpdateAnime by injectLazy()
    private val setAnimeCategories: SetAnimeCategories by injectLazy()
    private val getCategories: GetCategories by injectLazy()
    private val episodeRepository: EpisodeRepository by injectLazy()
    private val upsertHistory: UpsertHistory by injectLazy()
    private val removeHistory: RemoveHistory by injectLazy()
    private val getHistoryByAnimeId: GetHistoryByAnimeId by injectLazy()
    private val insertFavoriteEntryAlternative: InsertFavoriteEntryAlternative by injectLazy()

    /**
     * @param episodes Cannot be an empty list!
     *
     * @return Triple<Accepted, Discarded, HasNew>
     */
    suspend fun findAcceptedRootAndDiscardOthers(
        sourceId: Long,
        episodes: List<Episode>,
    ): Triple<EpisodeChain, List<EpisodeChain>, List<Episode>> {
        // Find other chains
        val chains = episodes
            .flatMap { episode ->
                getEpisodeByUrl.await(episode.url).map { it.animeId }
            }
            .distinct()
            .mapNotNull { animeId ->
                coroutineScope {
                    val anime = async(Dispatchers.IO) {
                        getAnime.await(animeId)
                    }
                    val episodeList = async(Dispatchers.IO) {
                        getEpisodesByAnimeId.await(animeId)
                    }
                    val history = async(Dispatchers.IO) {
                        getHistoryByAnimeId.await(animeId)
                    }
                    EpisodeChain(
                        anime.await() ?: return@coroutineScope null,
                        episodeList.await(),
                        history.await(),
                    )
                }
            }
            .filter { it.anime.source == sourceId }

        // Accept oldest chain
        val accepted = chains.minBy { it.anime.id }

        val toDiscard = chains.filter { it.anime.favorite && it.anime.id != accepted.anime.id }
        val animeUpdates = mutableListOf<AnimeUpdate>()

        val chainsAsEpisodes = chains.flatMap { it.episodes }
        val chainsAsHistory = chains.flatMap { it.history }

        return if (toDiscard.isNotEmpty()) {
            // Copy chain episodes to curEpisodes
            val (episodeUpdates, newEpisodes, new) = getEpisodeList(accepted, toDiscard, chainsAsEpisodes)

            toDiscard.forEach {
                animeUpdates += AnimeUpdate(
                    id = it.anime.id,
                    favorite = false,
                    dateAdded = 0,
                )
            }
            if (!accepted.anime.favorite) {
                animeUpdates += AnimeUpdate(
                    id = accepted.anime.id,
                    favorite = true,
                    dateAdded = System.currentTimeMillis(),
                )
            }

            val newAccepted = EpisodeChain(accepted.anime, newEpisodes, emptyList())
            val rootsToMutate = toDiscard + newAccepted

            // Apply changes to all anime
            updateAnime.awaitAll(animeUpdates)
            // Insert new episodes for accepted anime
            episodeRepository.updateAll(episodeUpdates)
            episodeRepository.addAll(newEpisodes)

            val (newHistory, deleteHistory) = getHistory(
                getEpisodesByAnimeId.await(accepted.anime.id),
                chainsAsEpisodes,
                chainsAsHistory,
            )

            // Delete the duplicate history first
            deleteHistory.forEach {
                removeHistory.awaitById(it)
            }

            // Insert new history
            newHistory.forEach {
                upsertHistory.await(it)
            }

            // Update favorites entry database
            val favoriteEntryUpdate = getFavoriteEntryAlternative(accepted, toDiscard)
            if (favoriteEntryUpdate != null) {
                insertFavoriteEntryAlternative.await(favoriteEntryUpdate)
            }

            // Copy categories from all chains to accepted anime

            val newCategories = rootsToMutate.flatMap { episodeChain ->
                getCategories.await(episodeChain.anime.id).map { it.id }
            }.distinct()
            rootsToMutate.forEach {
                setAnimeCategories.await(it.anime.id, newCategories)
            }

            Triple(newAccepted, toDiscard, newEpisodes)
        } else {
            /*val notNeeded = chains.filter { it.anime.id != accepted.anime.id }
            val (newEpisodes, new) = getEpisodeList(accepted, notNeeded, chainsAsEpisodes)
            val newAccepted = EpisodeChain(accepted.anime, newEpisodes)

            // Insert new episodes for accepted anime
            db.insertEpisodes(newAccepted.episodes).await()*/

            Triple(accepted, emptyList(), emptyList())
        }
    }

    private fun getFavoriteEntryAlternative(
        accepted: EpisodeChain,
        toDiscard: List<EpisodeChain>,
    ): FavoriteEntryAlternative? {
        val favorite = toDiscard.find { it.anime.favorite } ?: return null

        val gid = EHentaiSearchMetadata.galleryId(accepted.anime.url)
        val token = EHentaiSearchMetadata.galleryToken(accepted.anime.url)

        return FavoriteEntryAlternative(
            otherGid = gid,
            otherToken = token,
            gid = EHentaiSearchMetadata.galleryId(favorite.anime.url),
            token = EHentaiSearchMetadata.galleryToken(favorite.anime.url),
        )
    }

    private fun getHistory(
        currentEpisodes: List<Episode>,
        chainsAsEpisodes: List<Episode>,
        chainsAsHistory: List<History>,
    ): Pair<List<HistoryUpdate>, List<Long>> {
        val history = chainsAsHistory.groupBy { history -> chainsAsEpisodes.find { it.id == history.episodeId }?.url }
        val newHistory = currentEpisodes.mapNotNull { episode ->
            val newHistory = history[episode.url]
                ?.maxByOrNull {
                    it.seenAt?.time ?: 0
                }
                ?.takeIf { it.episodeId != episode.id && it.seenAt != null }
            if (newHistory != null) {
                HistoryUpdate(episode.id, newHistory.seenAt!!, newHistory.watchDuration)
            } else {
                null
            }
        }
        val currentEpisodeIds = currentEpisodes.map { it.id }
        val historyToDelete = chainsAsHistory.filterNot { it.episodeId in currentEpisodeIds }
            .map { it.id }
        return newHistory to historyToDelete
    }

    private fun getEpisodeList(
        accepted: EpisodeChain,
        toDiscard: List<EpisodeChain>,
        chainsAsEpisodes: List<Episode>,
    ): Triple<List<EpisodeUpdate>, List<Episode>, Boolean> {
        var new = false
        return toDiscard
            .flatMap { chain ->
                chain.episodes
            }
            .fold(accepted.episodes) { curEpisodes, episode ->
                val newLastPageRead = chainsAsEpisodes.maxOfOrNull { it.lastSecondSeen }

                if (curEpisodes.any { it.url == episode.url }) {
                    curEpisodes.map {
                        if (it.url == episode.url) {
                            val read = it.seen || episode.seen
                            var lastPageRead = it.lastSecondSeen.coerceAtLeast(episode.lastSecondSeen)
                            if (newLastPageRead != null && lastPageRead <= 0) {
                                lastPageRead = newLastPageRead
                            }
                            val bookmark = it.bookmark || episode.bookmark
                            it.copy(
                                seen = read,
                                lastSecondSeen = lastPageRead,
                                bookmark = bookmark,
                            )
                        } else {
                            it
                        }
                    }
                } else {
                    new = true
                    curEpisodes + Episode(
                        id = -1,
                        animeId = accepted.anime.id,
                        url = episode.url,
                        name = episode.name,
                        seen = episode.seen,
                        bookmark = episode.bookmark,
                        lastSecondSeen = if (newLastPageRead != null && episode.lastSecondSeen <= 0) {
                            newLastPageRead
                        } else {
                            episode.lastSecondSeen
                        },
                        dateFetch = episode.dateFetch,
                        dateUpload = episode.dateUpload,
                        episodeNumber = -1.0,
                        scanlator = null,
                        sourceOrder = -1,
                        lastModifiedAt = 0,
                        version = 0,
                    )
                }
            }
            .sortedBy { it.dateUpload }
            .let { episodes ->
                val updates = mutableListOf<EpisodeUpdate>()
                val newEpisodes = mutableListOf<Episode>()
                episodes.mapIndexed { index, episode ->
                    val name = "v${index + 1}: " + episode.name.substringAfter(" ")
                    val episodeNumber = index + 1.0
                    val sourceOrder = episodes.lastIndex - index.toLong()
                    when (episode.id) {
                        -1L -> newEpisodes.add(
                            episode.copy(
                                name = name,
                                episodeNumber = episodeNumber,
                                sourceOrder = sourceOrder,
                            ),
                        )
                        else -> updates.add(
                            EpisodeUpdate(
                                id = episode.id,
                                name = name.takeUnless { episode.name == it },
                                episodeNumber = episodeNumber.takeUnless { episode.episodeNumber == it },
                                sourceOrder = sourceOrder.takeUnless { episode.sourceOrder == it },
                            ),
                        )
                    }
                }
                Triple(updates.toList(), newEpisodes.toList(), new)
            }
    }
}

data class GalleryEntry(val gId: String, val gToken: String) {
    class Serializer : MemAutoFlushingLookupTable.EntrySerializer<GalleryEntry> {
        /**
         * Serialize an entry as a String.
         */
        override fun write(entry: GalleryEntry) = with(entry) { "$gId:$gToken" }

        /**
         * Read an entry from a String.
         */
        override fun read(string: String): GalleryEntry {
            val colonIndex = string.indexOf(':')
            return GalleryEntry(
                string.substring(0, colonIndex),
                string.substring(colonIndex + 1, string.length),
            )
        }
    }
}
