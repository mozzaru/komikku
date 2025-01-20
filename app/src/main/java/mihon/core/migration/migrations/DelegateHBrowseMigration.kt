package mihon.core.migration.migrations

import eu.kanade.domain.anime.interactor.UpdateAnime
import exh.source.HBROWSE_OLD_ID
import exh.source.HBROWSE_SOURCE_ID
import mihon.core.migration.MigrateUtils
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.anime.interactor.GetAnimeBySource
import tachiyomi.domain.anime.model.AnimeUpdate

class DelegateHBrowseMigration : Migration {
    override val version: Float = 4f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val getAnimeBySource = migrationContext.get<GetAnimeBySource>() ?: return@withIOContext false
        val updateAnime = migrationContext.get<UpdateAnime>() ?: return@withIOContext false
        MigrateUtils.updateSourceId(migrationContext, HBROWSE_SOURCE_ID, HBROWSE_OLD_ID)

        // Migrate BHrowse URLs
        val hBrowseManga = getAnimeBySource.await(HBROWSE_SOURCE_ID)
        val animeUpdates = hBrowseManga.map {
            AnimeUpdate(it.id, url = it.url + "/c00001/")
        }
        updateAnime.awaitAll(animeUpdates)
        return@withIOContext true
    }
}
