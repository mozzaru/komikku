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

data class ChapterChain(val manga: Anime, val episodes: List<Episode>, val history: List<History>)

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
    ): Triple<ChapterChain, List<ChapterChain>, List<Episode>> {
        // Find other chains
        val chains = episodes
            .flatMap { chapter ->
                getEpisodeByUrl.await(chapter.url).map { it.animeId }
            }
            .distinct()
            .mapNotNull { mangaId ->
                coroutineScope {
                    val manga = async(Dispatchers.IO) {
                        getAnime.await(mangaId)
                    }
                    val chapterList = async(Dispatchers.IO) {
                        getEpisodesByAnimeId.await(mangaId)
                    }
                    val history = async(Dispatchers.IO) {
                        getHistoryByAnimeId.await(mangaId)
                    }
                    ChapterChain(
                        manga.await() ?: return@coroutineScope null,
                        chapterList.await(),
                        history.await(),
                    )
                }
            }
            .filter { it.manga.source == sourceId }

        // Accept oldest chain
        val accepted = chains.minBy { it.manga.id }

        val toDiscard = chains.filter { it.manga.favorite && it.manga.id != accepted.manga.id }
        val animeUpdates = mutableListOf<AnimeUpdate>()

        val chainsAsChapters = chains.flatMap { it.episodes }
        val chainsAsHistory = chains.flatMap { it.history }

        return if (toDiscard.isNotEmpty()) {
            // Copy chain episodes to curChapters
            val (chapterUpdates, newChapters, new) = getChapterList(accepted, toDiscard, chainsAsChapters)

            toDiscard.forEach {
                animeUpdates += AnimeUpdate(
                    id = it.manga.id,
                    favorite = false,
                    dateAdded = 0,
                )
            }
            if (!accepted.manga.favorite) {
                animeUpdates += AnimeUpdate(
                    id = accepted.manga.id,
                    favorite = true,
                    dateAdded = System.currentTimeMillis(),
                )
            }

            val newAccepted = ChapterChain(accepted.manga, newChapters, emptyList())
            val rootsToMutate = toDiscard + newAccepted

            // Apply changes to all manga
            updateAnime.awaitAll(animeUpdates)
            // Insert new episodes for accepted manga
            episodeRepository.updateAll(chapterUpdates)
            episodeRepository.addAll(newChapters)

            val (newHistory, deleteHistory) = getHistory(
                getEpisodesByAnimeId.await(accepted.manga.id),
                chainsAsChapters,
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

            // Copy categories from all chains to accepted manga

            val newCategories = rootsToMutate.flatMap { chapterChain ->
                getCategories.await(chapterChain.manga.id).map { it.id }
            }.distinct()
            rootsToMutate.forEach {
                setAnimeCategories.await(it.manga.id, newCategories)
            }

            Triple(newAccepted, toDiscard, newChapters)
        } else {
            /*val notNeeded = chains.filter { it.manga.id != accepted.manga.id }
            val (newChapters, new) = getEpisodeList(accepted, notNeeded, chainsAsChapters)
            val newAccepted = ChapterChain(accepted.manga, newChapters)

            // Insert new episodes for accepted manga
            db.insertChapters(newAccepted.episodes).await()*/

            Triple(accepted, emptyList(), emptyList())
        }
    }

    private fun getFavoriteEntryAlternative(
        accepted: ChapterChain,
        toDiscard: List<ChapterChain>,
    ): FavoriteEntryAlternative? {
        val favorite = toDiscard.find { it.manga.favorite } ?: return null

        val gid = EHentaiSearchMetadata.galleryId(accepted.manga.url)
        val token = EHentaiSearchMetadata.galleryToken(accepted.manga.url)

        return FavoriteEntryAlternative(
            otherGid = gid,
            otherToken = token,
            gid = EHentaiSearchMetadata.galleryId(favorite.manga.url),
            token = EHentaiSearchMetadata.galleryToken(favorite.manga.url),
        )
    }

    private fun getHistory(
        currentEpisodes: List<Episode>,
        chainsAsEpisodes: List<Episode>,
        chainsAsHistory: List<History>,
    ): Pair<List<HistoryUpdate>, List<Long>> {
        val history = chainsAsHistory.groupBy { history -> chainsAsEpisodes.find { it.id == history.episodeId }?.url }
        val newHistory = currentEpisodes.mapNotNull { chapter ->
            val newHistory = history[chapter.url]
                ?.maxByOrNull {
                    it.seenAt?.time ?: 0
                }
                ?.takeIf { it.episodeId != chapter.id && it.seenAt != null }
            if (newHistory != null) {
                HistoryUpdate(chapter.id, newHistory.seenAt!!, newHistory.watchDuration)
            } else {
                null
            }
        }
        val currentChapterIds = currentEpisodes.map { it.id }
        val historyToDelete = chainsAsHistory.filterNot { it.episodeId in currentChapterIds }
            .map { it.id }
        return newHistory to historyToDelete
    }

    private fun getChapterList(
        accepted: ChapterChain,
        toDiscard: List<ChapterChain>,
        chainsAsEpisodes: List<Episode>,
    ): Triple<List<EpisodeUpdate>, List<Episode>, Boolean> {
        var new = false
        return toDiscard
            .flatMap { chain ->
                chain.episodes
            }
            .fold(accepted.episodes) { curChapters, chapter ->
                val newLastPageRead = chainsAsEpisodes.maxOfOrNull { it.lastSecondSeen }

                if (curChapters.any { it.url == chapter.url }) {
                    curChapters.map {
                        if (it.url == chapter.url) {
                            val read = it.seen || chapter.seen
                            var lastPageRead = it.lastSecondSeen.coerceAtLeast(chapter.lastSecondSeen)
                            if (newLastPageRead != null && lastPageRead <= 0) {
                                lastPageRead = newLastPageRead
                            }
                            val bookmark = it.bookmark || chapter.bookmark
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
                    curChapters + Episode(
                        id = -1,
                        animeId = accepted.manga.id,
                        url = chapter.url,
                        name = chapter.name,
                        seen = chapter.seen,
                        bookmark = chapter.bookmark,
                        lastSecondSeen = if (newLastPageRead != null && chapter.lastSecondSeen <= 0) {
                            newLastPageRead
                        } else {
                            chapter.lastSecondSeen
                        },
                        dateFetch = chapter.dateFetch,
                        dateUpload = chapter.dateUpload,
                        episodeNumber = -1.0,
                        scanlator = null,
                        sourceOrder = -1,
                        lastModifiedAt = 0,
                        version = 0,
                    )
                }
            }
            .sortedBy { it.dateUpload }
            .let { chapters ->
                val updates = mutableListOf<EpisodeUpdate>()
                val newEpisodes = mutableListOf<Episode>()
                chapters.mapIndexed { index, chapter ->
                    val name = "v${index + 1}: " + chapter.name.substringAfter(" ")
                    val chapterNumber = index + 1.0
                    val sourceOrder = chapters.lastIndex - index.toLong()
                    when (chapter.id) {
                        -1L -> newEpisodes.add(
                            chapter.copy(
                                name = name,
                                episodeNumber = chapterNumber,
                                sourceOrder = sourceOrder,
                            ),
                        )
                        else -> updates.add(
                            EpisodeUpdate(
                                id = chapter.id,
                                name = name.takeUnless { chapter.name == it },
                                episodeNumber = chapterNumber.takeUnless { chapter.episodeNumber == it },
                                sourceOrder = sourceOrder.takeUnless { chapter.sourceOrder == it },
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
