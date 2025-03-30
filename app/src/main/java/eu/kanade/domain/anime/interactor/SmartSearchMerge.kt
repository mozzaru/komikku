package eu.kanade.domain.anime.interactor

import android.app.Application
import eu.kanade.domain.anime.model.copyFrom
import eu.kanade.domain.anime.model.toSAnime
import exh.source.MERGED_SOURCE_ID
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.domain.manga.interactor.DeleteAnimeById
import tachiyomi.domain.manga.interactor.DeleteByMergeId
import tachiyomi.domain.manga.interactor.GetAnime
import tachiyomi.domain.manga.interactor.GetMergedReferencesById
import tachiyomi.domain.manga.interactor.InsertMergedReference
import tachiyomi.domain.manga.interactor.NetworkToLocalAnime
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MergedAnimeReference
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetAnimeCategories
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SmartSearchMerge(
    private val getAnime: GetAnime = Injekt.get(),
    private val getMergedReferencesById: GetMergedReferencesById = Injekt.get(),
    private val insertMergedReference: InsertMergedReference = Injekt.get(),
    private val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
    private val deleteAnimeById: DeleteAnimeById = Injekt.get(),
    private val deleteByMergeId: DeleteByMergeId = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val setAnimeCategories: SetAnimeCategories = Injekt.get(),
) {
    suspend fun smartSearchMerge(manga: Manga, originalAnimeId: Long): Manga {
        // KMK -->
        val context = Injekt.get<Application>()
        // KMK <--
        val originalAnime = getAnime.await(originalAnimeId)
            ?: throw IllegalArgumentException(context.stringResource(SYMR.strings.merge_unknown_entry, originalAnimeId))
        if (originalAnime.source == MERGED_SOURCE_ID) {
            val children = getMergedReferencesById.await(originalAnimeId)
            if (children.any { it.animeSourceId == manga.source && it.animeUrl == manga.url }) {
                // Merged already
                return originalAnime
            }

            val animeReferences = mutableListOf(
                MergedAnimeReference(
                    id = -1,
                    isInfoAnime = false,
                    getEpisodeUpdates = true,
                    episodeSortMode = 0,
                    episodePriority = 0,
                    downloadEpisodes = true,
                    mergeId = originalAnime.id,
                    mergeUrl = originalAnime.url,
                    animeId = manga.id,
                    animeUrl = manga.url,
                    animeSourceId = manga.source,
                ),
            )

            if (children.isEmpty() || children.all { it.animeSourceId != MERGED_SOURCE_ID }) {
                animeReferences += MergedAnimeReference(
                    id = -1,
                    isInfoAnime = false,
                    getEpisodeUpdates = false,
                    episodeSortMode = 0,
                    episodePriority = -1,
                    downloadEpisodes = false,
                    mergeId = originalAnime.id,
                    mergeUrl = originalAnime.url,
                    animeId = originalAnime.id,
                    animeUrl = originalAnime.url,
                    animeSourceId = MERGED_SOURCE_ID,
                )
            }

            // todo
            insertMergedReference.awaitAll(animeReferences)

            return originalAnime
        } else {
            if (manga.id == originalAnimeId) {
                // Merged already
                return originalAnime
            }
            var mergedManga = Manga.create()
                .copy(
                    url = originalAnime.url,
                    ogTitle = originalAnime.title,
                    source = MERGED_SOURCE_ID,
                )
                .copyFrom(originalAnime.toSAnime())
                .copy(
                    favorite = true,
                    lastUpdate = originalAnime.lastUpdate,
                    viewerFlags = originalAnime.viewerFlags,
                    chapterFlags = originalAnime.chapterFlags,
                    dateAdded = System.currentTimeMillis(),
                )

            var existingAnime = getAnime.await(mergedManga.url, mergedManga.source)
            while (existingAnime != null) {
                if (existingAnime.favorite) {
                    // Duplicate entry found -> use it instead
                    mergedManga = existingAnime
                    break
                } else {
                    withNonCancellableContext {
                        existingAnime?.id?.let {
                            deleteByMergeId.await(it)
                            deleteAnimeById.await(it)
                        }
                    }
                }
                // Remove previously merged entry from database (user already removed from favorites)
                existingAnime = getAnime.await(mergedManga.url, mergedManga.source)
            }

            mergedManga = networkToLocalAnime.await(mergedManga)

            getCategories.await(originalAnimeId)
                .let { categories ->
                    setAnimeCategories.await(mergedManga.id, categories.map { it.id })
                }

            val originalAnimeReference = MergedAnimeReference(
                id = -1,
                isInfoAnime = true,
                getEpisodeUpdates = true,
                episodeSortMode = 0,
                episodePriority = 0,
                downloadEpisodes = true,
                mergeId = mergedManga.id,
                mergeUrl = mergedManga.url,
                animeId = originalAnime.id,
                animeUrl = originalAnime.url,
                animeSourceId = originalAnime.source,
            )

            val newAnimeReference = MergedAnimeReference(
                id = -1,
                isInfoAnime = false,
                getEpisodeUpdates = true,
                episodeSortMode = 0,
                episodePriority = 0,
                downloadEpisodes = true,
                mergeId = mergedManga.id,
                mergeUrl = mergedManga.url,
                animeId = manga.id,
                animeUrl = manga.url,
                animeSourceId = manga.source,
            )

            val mergedAnimeReference = MergedAnimeReference(
                id = -1,
                isInfoAnime = false,
                getEpisodeUpdates = false,
                episodeSortMode = 0,
                episodePriority = -1,
                downloadEpisodes = false,
                mergeId = mergedManga.id,
                mergeUrl = mergedManga.url,
                animeId = mergedManga.id,
                animeUrl = mergedManga.url,
                animeSourceId = MERGED_SOURCE_ID,
            )

            insertMergedReference.awaitAll(listOf(originalAnimeReference, newAnimeReference, mergedAnimeReference))

            return mergedManga
        }

        // Note that if the anime are merged in a different order, this won't trigger, but I don't care lol
    }
}
