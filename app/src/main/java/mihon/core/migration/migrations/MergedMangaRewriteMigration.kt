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
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.AnimeUpdate
import tachiyomi.domain.anime.model.MergedAnimeReference
import tachiyomi.domain.episode.interactor.DeleteEpisodes
import tachiyomi.domain.episode.interactor.UpdateEpisode
import tachiyomi.domain.episode.model.EpisodeUpdate
import tachiyomi.domain.source.service.SourceManager

class MergedAnimeRewriteMigration : Migration {
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
        val mergedAnimes = getAnimeBySource.await(MERGED_SOURCE_ID)

        if (mergedAnimes.isNotEmpty()) {
            val animeConfigs = mergedAnimes.mapNotNull { mergedAnime ->
                readAnimeConfig(mergedAnime)?.let { mergedAnime to it }
            }
            if (animeConfigs.isNotEmpty()) {
                val animeToUpdate = mutableListOf<AnimeUpdate>()
                val mergedAnimeReferences = mutableListOf<MergedAnimeReference>()
                animeConfigs.onEach { mergedAnime ->
                    val newFirst = mergedAnime.second.children.firstOrNull()?.url?.let {
                        if (getAnime.await(it, MERGED_SOURCE_ID) != null) return@onEach
                        animeToUpdate += AnimeUpdate(id = mergedAnime.first.id, url = it)
                        mergedAnime.first.copy(url = it)
                    } ?: mergedAnime.first
                    mergedAnimeReferences += MergedAnimeReference(
                        id = -1,
                        isInfoAnime = false,
                        getEpisodeUpdates = false,
                        episodeSortMode = 0,
                        episodePriority = 0,
                        downloadEpisodes = false,
                        mergeId = newFirst.id,
                        mergeUrl = newFirst.url,
                        animeId = newFirst.id,
                        animeUrl = newFirst.url,
                        animeSourceId = MERGED_SOURCE_ID,
                    )
                    mergedAnime.second.children.distinct().forEachIndexed { index, animeSource ->
                        val load = animeSource.load(getAnime, sourceManager) ?: return@forEachIndexed
                        mergedAnimeReferences += MergedAnimeReference(
                            id = -1,
                            isInfoAnime = index == 0,
                            getEpisodeUpdates = true,
                            episodeSortMode = 0,
                            episodePriority = 0,
                            downloadEpisodes = true,
                            mergeId = newFirst.id,
                            mergeUrl = newFirst.url,
                            animeId = load.anime.id,
                            animeUrl = load.anime.url,
                            animeSourceId = load.source.id,
                        )
                    }
                }

                updateAnime.awaitAll(animeToUpdate)
                insertMergedReference.awaitAll(mergedAnimeReferences)

                val loadedAnimeList = animeConfigs
                    .map { it.second.children }
                    .flatten()
                    .mapNotNull { it.load(getAnime, sourceManager) }
                    .distinct()
                val episodes =
                    handler.awaitList {
                        ehQueries.getEpisodesByAnimeIds(
                            mergedAnimes.map { it.id },
                            EpisodeMapper::mapEpisode,
                        )
                    }

                val mergedAnimeEpisodes =
                    handler.awaitList {
                        ehQueries.getEpisodesByAnimeIds(
                            loadedAnimeList.map { it.anime.id },
                            EpisodeMapper::mapEpisode,
                        )
                    }

                val mergedAnimeEpisodesMatched = mergedAnimeEpisodes.mapNotNull { episode ->
                    loadedAnimeList.firstOrNull {
                        it.anime.id == episode.id
                    }?.let { it to episode }
                }
                val parsedEpisodes = episodes.filter {
                    it.seen || it.lastSecondSeen != 0L
                }.mapNotNull { episode -> readUrlConfig(episode.url)?.let { episode to it } }
                val episodesToUpdate = mutableListOf<EpisodeUpdate>()
                parsedEpisodes.forEach { parsedEpisode ->
                    mergedAnimeEpisodesMatched.firstOrNull {
                        it.second.url == parsedEpisode.second.url &&
                            it.first.source.id == parsedEpisode.second.source &&
                            it.first.anime.url == parsedEpisode.second.animeUrl
                    }?.let {
                        episodesToUpdate += EpisodeUpdate(
                            it.second.id,
                            seen = parsedEpisode.first.seen,
                            lastSecondSeen = parsedEpisode.first.lastSecondSeen,
                        )
                    }
                }

                deleteEpisodes.await(mergedAnimeEpisodes.map { it.id })
                updateEpisode.awaitAll(episodesToUpdate)
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
        val animeUrl: String,
    )

    @Serializable
    private data class AnimeConfig(
        @SerialName("c")
        val children: List<AnimeSource>,
    ) {
        companion object {
            fun readFromUrl(url: String): AnimeConfig? {
                return try {
                    Json.decodeFromString(url)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    private fun readAnimeConfig(anime: Anime): AnimeConfig? {
        return AnimeConfig.readFromUrl(anime.url)
    }

    @Serializable
    private data class AnimeSource(
        @SerialName("s")
        val source: Long,
        @SerialName("u")
        val url: String,
    ) {
        suspend fun load(getAnime: GetAnime, sourceManager: SourceManager): LoadedAnimeSource? {
            val anime = getAnime.await(url, source) ?: return null
            val source = sourceManager.getOrStub(source)
            return LoadedAnimeSource(source, anime)
        }
    }

    private fun readUrlConfig(url: String): UrlConfig? {
        return try {
            Json.decodeFromString(url)
        } catch (e: Exception) {
            null
        }
    }

    private data class LoadedAnimeSource(val source: Source, val anime: Anime)
}
