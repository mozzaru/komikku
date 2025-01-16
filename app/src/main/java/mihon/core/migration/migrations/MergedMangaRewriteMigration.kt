package mihon.core.migration.migrations

import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.tachiyomi.source.Source
import exh.source.MERGED_SOURCE_ID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.episode.EpisodeMapper
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.anime.interactor.GetAnimeBySource
import tachiyomi.domain.anime.interactor.InsertMergedReference
import tachiyomi.domain.anime.model.AnimeUpdate
import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.anime.model.MergedAnimeReference
import tachiyomi.domain.episode.interactor.DeleteEpisodes
import tachiyomi.domain.episode.interactor.UpdateEpisode
import tachiyomi.domain.episode.model.EpisodeUpdate
import tachiyomi.domain.source.service.SourceManager

class MergedMangaRewriteMigration : Migration {
    override val version: Float = 7f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val handler = migrationContext.get<DatabaseHandler>() ?: return@withIOContext false
        val getAnimeBySource = migrationContext.get<GetAnimeBySource>() ?: return@withIOContext false
        val getAnime = migrationContext.get<GetAnime>() ?: return@withIOContext false
        val updateAnime = migrationContext.get<UpdateAnime>() ?: return@withIOContext false
        val insertMergedReference = migrationContext.get<InsertMergedReference>() ?: return@withIOContext false
        val sourceManager = migrationContext.get<SourceManager>() ?: return@withIOContext false
        val deleteEpisodes = migrationContext.get<DeleteEpisodes>() ?: return@withIOContext false
        val updateEpisode = migrationContext.get<UpdateEpisode>() ?: return@withIOContext false
        val mergedMangas = getAnimeBySource.await(MERGED_SOURCE_ID)

        if (mergedMangas.isNotEmpty()) {
            val mangaConfigs = mergedMangas.mapNotNull { mergedManga ->
                readMangaConfig(mergedManga)?.let { mergedManga to it }
            }
            if (mangaConfigs.isNotEmpty()) {
                val mangaToUpdate = mutableListOf<AnimeUpdate>()
                val mergedAnimeReferences = mutableListOf<MergedAnimeReference>()
                mangaConfigs.onEach { mergedManga ->
                    val newFirst = mergedManga.second.children.firstOrNull()?.url?.let {
                        if (getAnime.await(it, MERGED_SOURCE_ID) != null) return@onEach
                        mangaToUpdate += AnimeUpdate(id = mergedManga.first.id, url = it)
                        mergedManga.first.copy(url = it)
                    } ?: mergedManga.first
                    mergedAnimeReferences += MergedAnimeReference(
                        id = -1,
                        isInfoManga = false,
                        getChapterUpdates = false,
                        chapterSortMode = 0,
                        chapterPriority = 0,
                        downloadChapters = false,
                        mergeId = newFirst.id,
                        mergeUrl = newFirst.url,
                        mangaId = newFirst.id,
                        mangaUrl = newFirst.url,
                        mangaSourceId = MERGED_SOURCE_ID,
                    )
                    mergedManga.second.children.distinct().forEachIndexed { index, mangaSource ->
                        val load = mangaSource.load(getAnime, sourceManager) ?: return@forEachIndexed
                        mergedAnimeReferences += MergedAnimeReference(
                            id = -1,
                            isInfoManga = index == 0,
                            getChapterUpdates = true,
                            chapterSortMode = 0,
                            chapterPriority = 0,
                            downloadChapters = true,
                            mergeId = newFirst.id,
                            mergeUrl = newFirst.url,
                            mangaId = load.manga.id,
                            mangaUrl = load.manga.url,
                            mangaSourceId = load.source.id,
                        )
                    }
                }

                updateAnime.awaitAll(mangaToUpdate)
                insertMergedReference.awaitAll(mergedAnimeReferences)

                val loadedMangaList = mangaConfigs
                    .map { it.second.children }
                    .flatten()
                    .mapNotNull { it.load(getAnime, sourceManager) }
                    .distinct()
                val chapters =
                    handler.awaitList {
                        ehQueries.getChaptersByMangaIds(
                            mergedMangas.map { it.id },
                            EpisodeMapper::mapChapter,
                        )
                    }

                val mergedMangaChapters =
                    handler.awaitList {
                        ehQueries.getChaptersByMangaIds(
                            loadedMangaList.map { it.manga.id },
                            EpisodeMapper::mapChapter,
                        )
                    }

                val mergedMangaChaptersMatched = mergedMangaChapters.mapNotNull { chapter ->
                    loadedMangaList.firstOrNull {
                        it.manga.id == chapter.id
                    }?.let { it to chapter }
                }
                val parsedChapters = chapters.filter {
                    it.read || it.lastPageRead != 0L
                }.mapNotNull { chapter -> readUrlConfig(chapter.url)?.let { chapter to it } }
                val chaptersToUpdate = mutableListOf<EpisodeUpdate>()
                parsedChapters.forEach { parsedChapter ->
                    mergedMangaChaptersMatched.firstOrNull {
                        it.second.url == parsedChapter.second.url &&
                            it.first.source.id == parsedChapter.second.source &&
                            it.first.manga.url == parsedChapter.second.mangaUrl
                    }?.let {
                        chaptersToUpdate += EpisodeUpdate(
                            it.second.id,
                            read = parsedChapter.first.read,
                            lastPageRead = parsedChapter.first.lastPageRead,
                        )
                    }
                }

                deleteEpisodes.await(mergedMangaChapters.map { it.id })
                updateEpisode.awaitAll(chaptersToUpdate)
            }
        }
        return@withIOContext true
    }

    @Serializable
    private data class UrlConfig(
        @SerialName("s")
        val source: Long,
        @SerialName("u")
        val url: String,
        @SerialName("m")
        val mangaUrl: String,
    )

    @Serializable
    private data class MangaConfig(
        @SerialName("c")
        val children: List<MangaSource>,
    ) {
        companion object {
            fun readFromUrl(url: String): MangaConfig? {
                return try {
                    Json.decodeFromString(url)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    private fun readMangaConfig(manga: Manga): MangaConfig? {
        return MangaConfig.readFromUrl(manga.url)
    }

    @Serializable
    private data class MangaSource(
        @SerialName("s")
        val source: Long,
        @SerialName("u")
        val url: String,
    ) {
        suspend fun load(getAnime: GetAnime, sourceManager: SourceManager): LoadedMangaSource? {
            val manga = getAnime.await(url, source) ?: return null
            val source = sourceManager.getOrStub(source)
            return LoadedMangaSource(source, manga)
        }
    }

    private fun readUrlConfig(url: String): UrlConfig? {
        return try {
            Json.decodeFromString(url)
        } catch (e: Exception) {
            null
        }
    }

    private data class LoadedMangaSource(val source: Source, val manga: Manga)
}
