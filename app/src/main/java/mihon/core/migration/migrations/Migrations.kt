package mihon.core.migration.migrations

import mihon.core.migration.Migration

val migrations: List<Migration>
    get() = listOf(
        SetupBackupCreateMigration(),
        SetupLibraryUpdateMigration(),
        SetupSyncDataMigration(),
        MergedMangaRewriteMigration(),
        LogoutFromMALMigration(),
        MoveDOHSettingMigration(),
        ResetRotationSettingMigration(),
        ResetReaderSettingsMigration(),
        RemoveOldReaderThemeMigration(),
        RemoveShorterLibraryUpdatesMigration(),
        MoveLibrarySortingSettingsMigration(),
        RemoveShortLibraryUpdatesMigration(),
        MoveLibraryNonCompleteSettingMigration(),
        MoveSecureScreenSettingMigration(),
        ChangeMiuiExtensionInstallerMigration(),
        MoveCoverOnlyGridSettingMigration(),
        MoveCatalogueCoverOnlyGridSettingMigration(),
        MoveLatestToFeedMigration(),
        MoveReaderTapSettingMigration(),
        MoveSortingModeSettingsMigration(),
        MoveSortingModeSettingMigration(),
        AlwaysBackupMigration(),
        ResetFilterAndSortSettingsMigration(),
        ChangeThemeModeToUppercaseMigration(),
        MoveReadingButtonSettingMigration(),
        ChangeTrackingQueueTypeMigration(),
        RemoveUpdateCheckerJobsMigration(),
        RemoveBatteryNotLowRestrictionMigration(),
        MoveRelativeTimeSettingMigration(),
        MoveSettingsToPrivateOrAppStateMigration(),
        MoveExtensionRepoSettingsMigration(),
        MoveCacheToDiskSettingMigration(),
        MoveEncryptionSettingsToAppStateMigration(),
        TrustExtensionRepositoryMigration(),
        // KMK -->
        OfficialExtensionRepositoryMigration(),
        SetupAppUpdateMigration(),
        // KMK <--
    )
