package eu.kanade.domain.episode.interactor

import eu.kanade.domain.anime.interactor.GetExcludedScanlators
import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.domain.anime.model.toSAnime
import eu.kanade.domain.episode.model.copyFromSEpisode
import eu.kanade.domain.episode.model.toSEpisode
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.online.HttpSource
import tachiyomi.data.episode.ChapterSanitizer
import tachiyomi.data.source.NoResultsException
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.ShouldUpdateDbEpisode
import tachiyomi.domain.chapter.interactor.UpdateEpisode
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.toEpisodeUpdate
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.chapter.service.EpisodeRecognition
import tachiyomi.source.local.isLocal
import java.lang.Long.max
import java.time.ZonedDateTime
import java.util.TreeSet

class SyncEpisodesWithSource(
    private val downloadManager: DownloadManager,
    private val downloadProvider: DownloadProvider,
    private val chapterRepository: ChapterRepository,
    private val shouldUpdateDbEpisode: ShouldUpdateDbEpisode,
    private val updateAnime: UpdateAnime,
    private val updateEpisode: UpdateEpisode,
    private val getChaptersByMangaId: GetChaptersByMangaId,
    private val getExcludedScanlators: GetExcludedScanlators,
) {

    /**
     * Method to synchronize db chapters with source ones
     *
     * @param rawSourceEpisodes the chapters from the source.
     * @param manga the manga the chapters belong to.
     * @param source the source the manga belongs to.
     * @return Newly added chapters
     */
    suspend fun await(
        rawSourceEpisodes: List<SChapter>,
        manga: Manga,
        source: Source,
        manualFetch: Boolean = false,
        fetchWindow: Pair<Long, Long> = Pair(0, 0),
    ): List<Chapter> {
        if (rawSourceEpisodes.isEmpty() && !source.isLocal()) {
            throw NoResultsException()
        }

        val now = ZonedDateTime.now()
        val nowMillis = now.toInstant().toEpochMilli()

        val sourceChapters = rawSourceEpisodes
            .distinctBy { it.url }
            .mapIndexed { i, sEpisode ->
                Chapter.create()
                    .copyFromSEpisode(sEpisode)
                    .copy(name = with(ChapterSanitizer) { sEpisode.name.sanitize(manga.title) })
                    .copy(animeId = manga.id, sourceOrder = i.toLong())
            }

        val dbEpisodes = getChaptersByMangaId.await(manga.id)

        val newChapters = mutableListOf<Chapter>()
        val updatedChapters = mutableListOf<Chapter>()
        val removedEpisodes = dbEpisodes.filterNot { dbEpisode ->
            sourceChapters.any { sourceEpisode ->
                dbEpisode.url == sourceEpisode.url
            }
        }

        // Used to not set upload date of older chapters
        // to a higher value than newer chapters
        var maxSeenUploadDate = 0L

        for (sourceEpisode in sourceChapters) {
            var episode = sourceEpisode

            // Update metadata from source if necessary.
            if (source is HttpSource) {
                val sEpisode = episode.toSEpisode()
                source.prepareNewChapter(sEpisode, manga.toSAnime())
                episode = episode.copyFromSEpisode(sEpisode)
            }

            // Recognize chapter number for the chapter.
            val episodeNumber = EpisodeRecognition.parseEpisodeNumber(
                manga.title,
                episode.name,
                episode.episodeNumber,
            )
            episode = episode.copy(episodeNumber = episodeNumber)

            val dbEpisode = dbEpisodes.find { it.url == episode.url }

            if (dbEpisode == null) {
                val toAddEpisode = if (episode.dateUpload == 0L) {
                    val altDateUpload = if (maxSeenUploadDate == 0L) nowMillis else maxSeenUploadDate
                    episode.copy(dateUpload = altDateUpload)
                } else {
                    maxSeenUploadDate = max(maxSeenUploadDate, sourceEpisode.dateUpload)
                    episode
                }
                newChapters.add(toAddEpisode)
            } else {
                if (shouldUpdateDbEpisode.await(dbEpisode, episode)) {
                    val shouldRenameEpisode = downloadProvider.isEpisodeDirNameChanged(dbEpisode, episode) &&
                        downloadManager.isEpisodeDownloaded(
                            dbEpisode.name,
                            dbEpisode.scanlator,
                            // SY -->
                            // manga.title,
                            manga.ogTitle,
                            // SY <--
                            manga.source,
                        )

                    if (shouldRenameEpisode) {
                        downloadManager.renameEpisode(source, manga, dbEpisode, episode)
                    }
                    var toChangeEpisode = dbEpisode.copy(
                        name = episode.name,
                        episodeNumber = episode.episodeNumber,
                        scanlator = episode.scanlator,
                        sourceOrder = episode.sourceOrder,
                    )
                    if (episode.dateUpload != 0L) {
                        toChangeEpisode = toChangeEpisode.copy(dateUpload = episode.dateUpload)
                    }
                    updatedChapters.add(toChangeEpisode)
                }
            }
        }

        // Return if there's nothing to add, delete, or update to avoid unnecessary db transactions.
        if (newChapters.isEmpty() && removedEpisodes.isEmpty() && updatedChapters.isEmpty()) {
            if (manualFetch || manga.fetchInterval == 0 || manga.nextUpdate < fetchWindow.first) {
                updateAnime.awaitUpdateFetchInterval(
                    manga,
                    now,
                    fetchWindow,
                )
            }
            return emptyList()
        }

        val reAdded = mutableListOf<Chapter>()

        val deletedEpisodeNumbers = TreeSet<Double>()
        val deletedSeenEpisodeNumbers = TreeSet<Double>()
        val deletedBookmarkedEpisodeNumbers = TreeSet<Double>()

        removedEpisodes.forEach { episode ->
            if (episode.seen) deletedSeenEpisodeNumbers.add(episode.episodeNumber)
            if (episode.bookmark) deletedBookmarkedEpisodeNumbers.add(episode.episodeNumber)
            deletedEpisodeNumbers.add(episode.episodeNumber)
        }

        val deletedEpisodeNumberDateFetchMap = removedEpisodes.sortedByDescending { it.dateFetch }
            .associate { it.episodeNumber to it.dateFetch }

        // Date fetch is set in such a way that the upper ones will have bigger value than the lower ones
        // Sources MUST return the chapters from most to less recent, which is common.
        var itemCount = newChapters.size
        var updatedToAdd = newChapters.map { toAddItem ->
            var episode = toAddItem.copy(dateFetch = nowMillis + itemCount--)

            if (!episode.isRecognizedNumber || episode.episodeNumber !in deletedEpisodeNumbers) return@map episode

            episode = episode.copy(
                seen = episode.episodeNumber in deletedSeenEpisodeNumbers,
                bookmark = episode.episodeNumber in deletedBookmarkedEpisodeNumbers,
            )

            // Try to to use the fetch date of the original entry to not pollute 'Updates' tab
            deletedEpisodeNumberDateFetchMap[episode.episodeNumber]?.let {
                episode = episode.copy(dateFetch = it)
            }

            reAdded.add(episode)

            episode
        }

        if (removedEpisodes.isNotEmpty()) {
            val toDeleteIds = removedEpisodes.map { it.id }
            chapterRepository.removeEpisodesWithIds(toDeleteIds)
        }

        if (updatedToAdd.isNotEmpty()) {
            updatedToAdd = chapterRepository.addAll(updatedToAdd)
        }

        if (updatedChapters.isNotEmpty()) {
            val episodeUpdates = updatedChapters.map { it.toEpisodeUpdate() }
            updateEpisode.awaitAll(episodeUpdates)
        }
        updateAnime.awaitUpdateFetchInterval(manga, now, fetchWindow)

        // Set this manga as updated since chapters were changed
        // Note that last_update actually represents last time the chapter list changed at all
        updateAnime.awaitUpdateLastUpdate(manga.id)

        val reAddedUrls = reAdded.map { it.url }.toHashSet()

        val excludedScanlators = getExcludedScanlators.await(manga.id).toHashSet()

        return updatedToAdd.filterNot {
            it.url in reAddedUrls || it.scanlator in excludedScanlators
        }
    }
}
