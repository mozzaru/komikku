package eu.kanade.domain.anime.interactor

import android.app.Application
import eu.kanade.domain.anime.model.copyFrom
import eu.kanade.domain.anime.model.toSManga
import exh.source.MERGED_SOURCE_ID
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.domain.anime.interactor.DeleteAnimeById
import tachiyomi.domain.anime.interactor.DeleteByMergeId
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.anime.interactor.GetMergedReferencesById
import tachiyomi.domain.anime.interactor.InsertMergedReference
import tachiyomi.domain.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.MergedAnimeReference
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
    suspend fun smartSearchMerge(anime: Anime, originalMangaId: Long): Anime {
        // KMK -->
        val context = Injekt.get<Application>()
        // KMK <--
        val originalManga = getAnime.await(originalMangaId)
            ?: throw IllegalArgumentException(context.stringResource(SYMR.strings.merge_unknown_entry, originalMangaId))
        if (originalManga.source == MERGED_SOURCE_ID) {
            val children = getMergedReferencesById.await(originalMangaId)
            if (children.any { it.mangaSourceId == anime.source && it.mangaUrl == anime.url }) {
                // Merged already
                return originalManga
            }

            val mangaReferences = mutableListOf(
                MergedAnimeReference(
                    id = -1,
                    isInfoManga = false,
                    getChapterUpdates = true,
                    chapterSortMode = 0,
                    chapterPriority = 0,
                    downloadChapters = true,
                    mergeId = originalManga.id,
                    mergeUrl = originalManga.url,
                    mangaId = anime.id,
                    mangaUrl = anime.url,
                    mangaSourceId = anime.source,
                ),
            )

            if (children.isEmpty() || children.all { it.mangaSourceId != MERGED_SOURCE_ID }) {
                mangaReferences += MergedAnimeReference(
                    id = -1,
                    isInfoManga = false,
                    getChapterUpdates = false,
                    chapterSortMode = 0,
                    chapterPriority = -1,
                    downloadChapters = false,
                    mergeId = originalManga.id,
                    mergeUrl = originalManga.url,
                    mangaId = originalManga.id,
                    mangaUrl = originalManga.url,
                    mangaSourceId = MERGED_SOURCE_ID,
                )
            }

            // todo
            insertMergedReference.awaitAll(mangaReferences)

            return originalManga
        } else {
            if (anime.id == originalMangaId) {
                // Merged already
                return originalManga
            }
            var mergedAnime = Anime.create()
                .copy(
                    url = originalManga.url,
                    ogTitle = originalManga.title,
                    source = MERGED_SOURCE_ID,
                )
                .copyFrom(originalManga.toSManga())
                .copy(
                    favorite = true,
                    lastUpdate = originalManga.lastUpdate,
                    viewerFlags = originalManga.viewerFlags,
                    chapterFlags = originalManga.chapterFlags,
                    dateAdded = System.currentTimeMillis(),
                )

            var existingManga = getAnime.await(mergedAnime.url, mergedAnime.source)
            while (existingManga != null) {
                if (existingManga.favorite) {
                    // Duplicate entry found -> use it instead
                    mergedAnime = existingManga
                    break
                } else {
                    withNonCancellableContext {
                        existingManga?.id?.let {
                            deleteByMergeId.await(it)
                            deleteAnimeById.await(it)
                        }
                    }
                }
                // Remove previously merged entry from database (user already removed from favorites)
                existingManga = getAnime.await(mergedAnime.url, mergedAnime.source)
            }

            mergedAnime = networkToLocalAnime.await(mergedAnime)

            getCategories.await(originalMangaId)
                .let { categories ->
                    setAnimeCategories.await(mergedAnime.id, categories.map { it.id })
                }

            val originalMangaReference = MergedAnimeReference(
                id = -1,
                isInfoManga = true,
                getChapterUpdates = true,
                chapterSortMode = 0,
                chapterPriority = 0,
                downloadChapters = true,
                mergeId = mergedAnime.id,
                mergeUrl = mergedAnime.url,
                mangaId = originalManga.id,
                mangaUrl = originalManga.url,
                mangaSourceId = originalManga.source,
            )

            val newMangaReference = MergedAnimeReference(
                id = -1,
                isInfoManga = false,
                getChapterUpdates = true,
                chapterSortMode = 0,
                chapterPriority = 0,
                downloadChapters = true,
                mergeId = mergedAnime.id,
                mergeUrl = mergedAnime.url,
                mangaId = anime.id,
                mangaUrl = anime.url,
                mangaSourceId = anime.source,
            )

            val mergedAnimeReference = MergedAnimeReference(
                id = -1,
                isInfoManga = false,
                getChapterUpdates = false,
                chapterSortMode = 0,
                chapterPriority = -1,
                downloadChapters = false,
                mergeId = mergedAnime.id,
                mergeUrl = mergedAnime.url,
                mangaId = mergedAnime.id,
                mangaUrl = mergedAnime.url,
                mangaSourceId = MERGED_SOURCE_ID,
            )

            insertMergedReference.awaitAll(listOf(originalMangaReference, newMangaReference, mergedAnimeReference))

            return mergedAnime
        }

        // Note that if the anime are merged in a different order, this won't trigger, but I don't care lol
    }
}
