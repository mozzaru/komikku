package mihon.core.migration.migrations

import eu.kanade.domain.anime.interactor.UpdateAnime
import exh.source.HBROWSE_OLD_ID
import exh.source.HBROWSE_SOURCE_ID
import mihon.core.migration.MigrateUtils
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.anime.interactor.GetMangaBySource
import tachiyomi.domain.anime.model.MangaUpdate

class DelegateHBrowseMigration : Migration {
    override val version: Float = 4f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val getMangaBySource = migrationContext.get<GetMangaBySource>() ?: return@withIOContext false
        val updateAnime = migrationContext.get<UpdateAnime>() ?: return@withIOContext false
        MigrateUtils.updateSourceId(migrationContext, HBROWSE_SOURCE_ID, HBROWSE_OLD_ID)

        // Migrate BHrowse URLs
        val hBrowseManga = getMangaBySource.await(HBROWSE_SOURCE_ID)
        val mangaUpdates = hBrowseManga.map {
            MangaUpdate(it.id, url = it.url + "/c00001/")
        }
        updateAnime.awaitAll(mangaUpdates)
        return@withIOContext true
    }
}
