package tachiyomi.domain.anime.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.anime.model.MergeAnimeSettingsUpdate
import tachiyomi.domain.anime.model.MergedAnimeReference

interface AnimeMergeRepository {
    suspend fun getMergedManga(): List<Manga>

    suspend fun subscribeMergedManga(): Flow<List<Manga>>

    suspend fun getMergedMangaById(id: Long): List<Manga>

    suspend fun subscribeMergedMangaById(id: Long): Flow<List<Manga>>

    suspend fun getReferencesById(id: Long): List<MergedAnimeReference>

    suspend fun subscribeReferencesById(id: Long): Flow<List<MergedAnimeReference>>

    suspend fun updateSettings(update: MergeAnimeSettingsUpdate): Boolean

    suspend fun updateAllSettings(values: List<MergeAnimeSettingsUpdate>): Boolean

    suspend fun insert(reference: MergedAnimeReference): Long?

    suspend fun insertAll(references: List<MergedAnimeReference>)

    suspend fun deleteById(id: Long)

    suspend fun deleteByMergeId(mergeId: Long)

    suspend fun getMergeMangaForDownloading(mergeId: Long): List<Manga>
}
