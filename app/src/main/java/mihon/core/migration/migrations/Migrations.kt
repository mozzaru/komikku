package mihon.core.migration.migrations

import mihon.core.migration.Migration

val migrations: List<Migration>
    get() = listOf(
        SetupBackupCreateMigration(),
        SetupLibraryUpdateMigration(),
        SetupEHentaiUpdateMigration(),
        SetupSyncDataMigration(),
        // KMK -->
        SetupAppUpdateMigration(),
        // KMK <--
    )
