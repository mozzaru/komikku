package eu.kanade.domain

import eu.kanade.domain.anime.interactor.GetExcludedScanlators
import eu.kanade.domain.anime.interactor.SetAnimeViewerFlags
import eu.kanade.domain.anime.interactor.SetExcludedScanlators
import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.domain.download.interactor.DeleteDownload
import eu.kanade.domain.episode.interactor.GetAvailableScanlators
import eu.kanade.domain.episode.interactor.SetSeenStatus
import eu.kanade.domain.episode.interactor.SyncEpisodesWithSource
import eu.kanade.domain.extension.interactor.GetExtensionLanguages
import eu.kanade.domain.extension.interactor.GetExtensionSources
import eu.kanade.domain.extension.interactor.GetExtensionsByType
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.domain.source.interactor.GetEnabledSources
import eu.kanade.domain.source.interactor.GetLanguagesWithSources
import eu.kanade.domain.source.interactor.GetSourcesWithFavoriteCount
import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.domain.source.interactor.ToggleLanguage
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.domain.source.interactor.ToggleSourcePin
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.domain.track.interactor.RefreshTracks
import eu.kanade.domain.track.interactor.SyncEpisodeProgressWithTrack
import eu.kanade.domain.track.interactor.TrackEpisode
import mihon.data.repository.ExtensionRepoRepositoryImpl
import mihon.domain.chapter.interactor.FilterChaptersForDownload
import mihon.domain.extensionrepo.interactor.CreateExtensionRepo
import mihon.domain.extensionrepo.interactor.DeleteExtensionRepo
import mihon.domain.extensionrepo.interactor.GetExtensionRepo
import mihon.domain.extensionrepo.interactor.GetExtensionRepoCount
import mihon.domain.extensionrepo.interactor.ReplaceExtensionRepo
import mihon.domain.extensionrepo.interactor.UpdateExtensionRepo
import mihon.domain.extensionrepo.repository.ExtensionRepoRepository
import mihon.domain.extensionrepo.service.ExtensionRepoService
import mihon.domain.upcoming.interactor.GetUpcomingManga
import tachiyomi.data.anime.MangaRepositoryImpl
import tachiyomi.data.category.CategoryRepositoryImpl
import tachiyomi.data.episode.ChapterRepositoryImpl
import tachiyomi.data.history.HistoryRepositoryImpl
import tachiyomi.data.release.ReleaseServiceImpl
import tachiyomi.data.source.SourceRepositoryImpl
import tachiyomi.data.source.StubSourceRepositoryImpl
import tachiyomi.data.track.TrackRepositoryImpl
import tachiyomi.data.updates.UpdatesRepositoryImpl
import tachiyomi.domain.manga.interactor.FetchInterval
import tachiyomi.domain.manga.interactor.GetAnime
import tachiyomi.domain.manga.interactor.GetAnimeByUrlAndSourceId
import tachiyomi.domain.manga.interactor.GetAnimeWithEpisodes
import tachiyomi.domain.manga.interactor.GetDuplicateLibraryAnime
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.NetworkToLocalAnime
import tachiyomi.domain.manga.interactor.ResetViewerFlags
import tachiyomi.domain.manga.interactor.SetAnimeEpisodeFlags
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.category.interactor.CreateCategoryWithName
import tachiyomi.domain.category.interactor.DeleteCategory
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.HideCategory
import tachiyomi.domain.category.interactor.RenameCategory
import tachiyomi.domain.category.interactor.ReorderCategory
import tachiyomi.domain.category.interactor.ResetCategoryFlags
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.category.interactor.SetDisplayMode
import tachiyomi.domain.category.interactor.SetSortModeForCategory
import tachiyomi.domain.category.interactor.UpdateCategory
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.chapter.interactor.GetEpisode
import tachiyomi.domain.chapter.interactor.GetEpisodeByUrlAndAnimeId
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.SetAnimeDefaultEpisodeFlags
import tachiyomi.domain.chapter.interactor.ShouldUpdateDbEpisode
import tachiyomi.domain.chapter.interactor.UpdateEpisode
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.GetNextEpisodes
import tachiyomi.domain.history.interactor.GetTotalWatchDuration
import tachiyomi.domain.history.interactor.RemoveHistory
import tachiyomi.domain.history.interactor.UpsertHistory
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.release.interactor.GetApplicationRelease
import tachiyomi.domain.release.service.ReleaseService
import tachiyomi.domain.source.interactor.GetRemoteAnime
import tachiyomi.domain.source.interactor.GetSourcesWithNonLibraryAnime
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.domain.track.interactor.DeleteTrack
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.GetTracksPerAnime
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.track.repository.TrackRepository
import tachiyomi.domain.updates.interactor.GetUpdates
import tachiyomi.domain.updates.repository.UpdatesRepository
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addFactory
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

class DomainModule : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addSingletonFactory<CategoryRepository> { CategoryRepositoryImpl(get()) }
        addFactory { GetCategories(get()) }
        addFactory { ResetCategoryFlags(get(), get()) }
        addFactory { SetDisplayMode(get()) }
        addFactory { SetSortModeForCategory(get(), get()) }
        addFactory { CreateCategoryWithName(get(), get()) }
        addFactory { RenameCategory(get()) }
        addFactory { ReorderCategory(get()) }
        addFactory { UpdateCategory(get()) }
        addFactory { DeleteCategory(get()) }
        // KMK -->
        addFactory { HideCategory(get()) }
        // KMK <--

        addSingletonFactory<MangaRepository> { MangaRepositoryImpl(get()) }
        addFactory { GetDuplicateLibraryAnime(get()) }
        addFactory { GetFavorites(get()) }
        addFactory { GetLibraryManga(get()) }
        addFactory { GetAnimeWithEpisodes(get(), get()) }
        addFactory { GetAnimeByUrlAndSourceId(get()) }
        addFactory { GetAnime(get()) }
        addFactory { GetNextEpisodes(get(), get(), get(), get()) }
        addFactory { GetUpcomingManga(get()) }
        addFactory { ResetViewerFlags(get()) }
        addFactory { SetAnimeEpisodeFlags(get()) }
        addFactory { FetchInterval(get()) }
        addFactory { SetAnimeDefaultEpisodeFlags(get(), get(), get()) }
        addFactory { SetAnimeViewerFlags(get()) }
        addFactory { NetworkToLocalAnime(get()) }
        addFactory { UpdateAnime(get(), get()) }
        addFactory { SetMangaCategories(get()) }
        addFactory { GetExcludedScanlators(get()) }
        addFactory { SetExcludedScanlators(get()) }

        addSingletonFactory<ReleaseService> { ReleaseServiceImpl(get(), get()) }
        addFactory { GetApplicationRelease(get(), get()) }

        addSingletonFactory<TrackRepository> { TrackRepositoryImpl(get()) }
        addFactory { TrackEpisode(get(), get(), get(), get()) }
        addFactory { AddTracks(get(), get(), get(), get()) }
        addFactory { RefreshTracks(get(), get(), get(), get()) }
        addFactory { DeleteTrack(get()) }
        addFactory { GetTracksPerAnime(get(), get()) }
        addFactory { GetTracks(get()) }
        addFactory { InsertTrack(get()) }
        addFactory { SyncEpisodeProgressWithTrack(get(), get(), get()) }

        addSingletonFactory<ChapterRepository> { ChapterRepositoryImpl(get()) }
        addFactory { GetEpisode(get()) }
        addFactory { GetChaptersByMangaId(get()) }
        addFactory { GetEpisodeByUrlAndAnimeId(get()) }
        addFactory { UpdateEpisode(get()) }
        addFactory { SetSeenStatus(get(), get(), get(), get(), get()) }
        addFactory { ShouldUpdateDbEpisode() }
        addFactory { SyncEpisodesWithSource(get(), get(), get(), get(), get(), get(), get(), get()) }
        addFactory { GetAvailableScanlators(get()) }
        addFactory { FilterChaptersForDownload(get(), get(), get(), get()) }

        addSingletonFactory<HistoryRepository> { HistoryRepositoryImpl(get()) }
        addFactory { GetHistory(get()) }
        addFactory { UpsertHistory(get()) }
        addFactory { RemoveHistory(get()) }
        addFactory { GetTotalWatchDuration(get()) }

        addFactory { DeleteDownload(get(), get()) }

        addFactory { GetExtensionsByType(get(), get()) }
        addFactory { GetExtensionSources(get()) }
        addFactory { GetExtensionLanguages(get(), get()) }

        addSingletonFactory<UpdatesRepository> { UpdatesRepositoryImpl(get()) }
        addFactory { GetUpdates(get()) }

        addSingletonFactory<SourceRepository> { SourceRepositoryImpl(get(), get()) }
        addSingletonFactory<StubSourceRepository> { StubSourceRepositoryImpl(get()) }
        addFactory { GetEnabledSources(get(), get()) }
        addFactory { GetLanguagesWithSources(get(), get()) }
        addFactory { GetRemoteAnime(get()) }
        addFactory { GetSourcesWithFavoriteCount(get(), get()) }
        addFactory { GetSourcesWithNonLibraryAnime(get()) }
        addFactory { SetMigrateSorting(get()) }
        addFactory { ToggleLanguage(get()) }
        addFactory { ToggleSource(get()) }
        addFactory { ToggleSourcePin(get()) }
        addFactory { TrustExtension(get(), get()) }

        addSingletonFactory<ExtensionRepoRepository> { ExtensionRepoRepositoryImpl(get()) }
        addFactory { ExtensionRepoService(get(), get()) }
        addFactory { GetExtensionRepo(get()) }
        addFactory { GetExtensionRepoCount(get()) }
        addFactory { CreateExtensionRepo(get(), get()) }
        addFactory { DeleteExtensionRepo(get()) }
        addFactory { ReplaceExtensionRepo(get()) }
        addFactory { UpdateExtensionRepo(get(), get()) }
    }
}
