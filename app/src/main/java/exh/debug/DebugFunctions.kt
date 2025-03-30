package exh.debug

import android.app.Application
import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.sync.SyncDataJob
import eu.kanade.tachiyomi.util.system.workManager
import exh.util.jobScheduler
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.protobuf.schema.ProtoBufSchemaGenerator
import mihon.core.migration.MigrationContext
import mihon.core.migration.MigrationJobFactory
import mihon.core.migration.MigrationStrategyFactory
import mihon.core.migration.Migrator
import mihon.core.migration.migrations.migrations
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.manga.interactor.GetAllAnime
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.UUID

@Suppress("unused")
object DebugFunctions {
    private val app: Application by injectLazy()
    private val handler: DatabaseHandler by injectLazy()
    private val sourceManager: SourceManager by injectLazy()
    private val updateAnime: UpdateAnime by injectLazy()
    private val getFavorites: GetFavorites by injectLazy()
    private val getAllAnime: GetAllAnime by injectLazy()

    fun forceUpgradeMigration(): Boolean {
        val migrationContext = MigrationContext(dryrun = false)
        val migrationJobFactory = MigrationJobFactory(migrationContext, Migrator.scope)
        val migrationStrategyFactory = MigrationStrategyFactory(migrationJobFactory) {}
        val strategy = migrationStrategyFactory.create(1, BuildConfig.VERSION_CODE)
        return runBlocking { strategy(migrations).await() }
    }

    fun forceSetupJobs(): Boolean {
        val migrationContext = MigrationContext(dryrun = false)
        val migrationJobFactory = MigrationJobFactory(migrationContext, Migrator.scope)
        val migrationStrategyFactory = MigrationStrategyFactory(migrationJobFactory) {}
        val strategy = migrationStrategyFactory.create(0, BuildConfig.VERSION_CODE)
        return runBlocking { strategy(migrations).await() }
    }

    fun addAllMangaInDatabaseToLibrary() {
        runBlocking { handler.await { ehQueries.addAllAnimeInDatabaseToLibrary() } }
    }

    fun countMangaInDatabaseInLibrary() = runBlocking { getFavorites.await().size }

    fun countMangaInDatabaseNotInLibrary() = runBlocking { getAllAnime.await() }.count { !it.favorite }

    fun countMangaInDatabase() = runBlocking { getAllAnime.await() }.size

    fun clearSavedSearches() = runBlocking { handler.await { saved_searchQueries.deleteAll() } }

    fun listAllSources() = sourceManager.getCatalogueSources().joinToString("\n") {
        "${it.id}: ${it.name} (${it.lang.uppercase()})"
    }

    fun listAllSourcesClassName() = sourceManager.getCatalogueSources().joinToString("\n") {
        "${it::class.qualifiedName}: ${it.name} (${it.lang.uppercase()})"
    }

    fun listVisibleSources() = sourceManager.getVisibleCatalogueSources().joinToString("\n") {
        "${it.id}: ${it.name} (${it.lang.uppercase()})"
    }

    fun listAllHttpSources() = sourceManager.getOnlineSources().joinToString("\n") {
        "${it.id}: ${it.name} (${it.lang.uppercase()})"
    }
    fun listVisibleHttpSources() = sourceManager.getVisibleOnlineSources().joinToString("\n") {
        "${it.id}: ${it.name} (${it.lang.uppercase()})"
    }

    fun listScheduledJobs() = app.jobScheduler.allPendingJobs.joinToString(",\n") { j ->
        val info = j.extras.getString("EXTRA_WORK_SPEC_ID")?.let {
            app.workManager.getWorkInfoById(UUID.fromString(it)).get()
        }

        if (info != null) {
            """
            {
                id: ${info.id},
                isPeriodic: ${j.extras.getBoolean("EXTRA_IS_PERIODIC")},
                state: ${info.state.name},
                tags: [
                    ${info.tags.joinToString(separator = ",\n                    ")}
                ],
            }
            """.trimIndent()
        } else {
            """
            {
                info: ${j.id},
                isPeriodic: ${j.isPeriodic},
                isPersisted: ${j.isPersisted},
                intervalMillis: ${j.intervalMillis},
            }
            """.trimIndent()
        }
    }

    fun cancelAllScheduledJobs() = app.jobScheduler.cancelAll()

    private fun convertSources(from: Long, to: Long) {
        runBlocking {
            handler.await { ehQueries.migrateSource(to, from) }
        }
    }

    fun exportProtobufScheme() = ProtoBufSchemaGenerator.generateSchemaText(Backup.serializer().descriptor)

    fun killSyncJobs() {
        val context = Injekt.get<Application>()
        SyncDataJob.stop(context)
    }

    fun killLibraryJobs() {
        val context = Injekt.get<Application>()
        LibraryUpdateJob.stop(context)
    }
}
