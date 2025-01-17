package eu.kanade.domain

import android.app.Application
import eu.kanade.domain.anime.interactor.CreateSortTag
import eu.kanade.domain.anime.interactor.DeleteSortTag
import eu.kanade.domain.anime.interactor.GetPagePreviews
import eu.kanade.domain.anime.interactor.GetSortTag
import eu.kanade.domain.anime.interactor.ReorderSortTag
import eu.kanade.domain.anime.interactor.SmartSearchMerge
import eu.kanade.domain.source.interactor.CreateSourceCategory
import eu.kanade.domain.source.interactor.DeleteSourceCategory
import eu.kanade.domain.source.interactor.GetExhSavedSearch
import eu.kanade.domain.source.interactor.GetShowLatest
import eu.kanade.domain.source.interactor.GetSourceCategories
import eu.kanade.domain.source.interactor.RenameSourceCategory
import eu.kanade.domain.source.interactor.SetSourceCategories
import eu.kanade.domain.source.interactor.ToggleExcludeFromDataSaver
import eu.kanade.tachiyomi.animesource.online.MetadataSource
import exh.search.SearchEngine
import tachiyomi.data.anime.AnimeMergeRepositoryImpl
import tachiyomi.data.anime.AnimeMetadataRepositoryImpl
import tachiyomi.data.anime.CustomAnimeRepositoryImpl
import tachiyomi.data.anime.FavoritesEntryRepositoryImpl
import tachiyomi.data.source.FeedSavedSearchRepositoryImpl
import tachiyomi.data.source.SavedSearchRepositoryImpl
import tachiyomi.domain.anime.interactor.DeleteAnimeById
import tachiyomi.domain.anime.interactor.DeleteByMergeId
import tachiyomi.domain.anime.interactor.DeleteFavoriteEntries
import tachiyomi.domain.anime.interactor.DeleteMergeById
import tachiyomi.domain.anime.interactor.GetAllAnime
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.anime.interactor.GetAnimeBySource
import tachiyomi.domain.anime.interactor.GetCustomAnimeInfo
import tachiyomi.domain.anime.interactor.GetExhFavoriteAnimeWithMetadata
import tachiyomi.domain.anime.interactor.GetFavoriteEntries
import tachiyomi.domain.anime.interactor.GetFlatMetadataById
import tachiyomi.domain.anime.interactor.GetIdsOfFavoriteAnimeWithMetadata
import tachiyomi.domain.anime.interactor.GetMergedAnime
import tachiyomi.domain.anime.interactor.GetMergedAnimeById
import tachiyomi.domain.anime.interactor.GetMergedAnimeForDownloading
import tachiyomi.domain.anime.interactor.GetMergedReferencesById
import tachiyomi.domain.anime.interactor.GetSearchMetadata
import tachiyomi.domain.anime.interactor.GetSearchTags
import tachiyomi.domain.anime.interactor.GetSearchTitles
import tachiyomi.domain.anime.interactor.GetSeenAnimeNotInLibraryView
import tachiyomi.domain.anime.interactor.InsertFavoriteEntries
import tachiyomi.domain.anime.interactor.InsertFavoriteEntryAlternative
import tachiyomi.domain.anime.interactor.InsertFlatMetadata
import tachiyomi.domain.anime.interactor.InsertMergedReference
import tachiyomi.domain.anime.interactor.SetCustomAnimeInfo
import tachiyomi.domain.anime.interactor.UpdateMergedSettings
import tachiyomi.domain.anime.repository.AnimeMergeRepository
import tachiyomi.domain.anime.repository.AnimeMetadataRepository
import tachiyomi.domain.anime.repository.CustomAnimeRepository
import tachiyomi.domain.anime.repository.FavoritesEntryRepository
import tachiyomi.domain.episode.interactor.DeleteEpisodes
import tachiyomi.domain.episode.interactor.GetEpisodeByUrl
import tachiyomi.domain.episode.interactor.GetMergedEpisodesByAnimeId
import tachiyomi.domain.history.interactor.GetHistoryByAnimeId
import tachiyomi.domain.source.interactor.CountFeedSavedSearchBySourceId
import tachiyomi.domain.source.interactor.CountFeedSavedSearchGlobal
import tachiyomi.domain.source.interactor.DeleteFeedSavedSearchById
import tachiyomi.domain.source.interactor.DeleteSavedSearchById
import tachiyomi.domain.source.interactor.GetFeedSavedSearchBySourceId
import tachiyomi.domain.source.interactor.GetFeedSavedSearchGlobal
import tachiyomi.domain.source.interactor.GetSavedSearchById
import tachiyomi.domain.source.interactor.GetSavedSearchBySourceId
import tachiyomi.domain.source.interactor.GetSavedSearchBySourceIdFeed
import tachiyomi.domain.source.interactor.GetSavedSearchGlobalFeed
import tachiyomi.domain.source.interactor.InsertFeedSavedSearch
import tachiyomi.domain.source.interactor.InsertSavedSearch
import tachiyomi.domain.source.interactor.ReorderFeed
import tachiyomi.domain.source.repository.FeedSavedSearchRepository
import tachiyomi.domain.source.repository.SavedSearchRepository
import tachiyomi.domain.track.interactor.IsTrackUnfollowed
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addFactory
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get
import xyz.nulldev.ts.api.http.serializer.FilterSerializer

class SYDomainModule : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addFactory { GetShowLatest(get()) }
        addFactory { ToggleExcludeFromDataSaver(get()) }
        addFactory { SetSourceCategories(get()) }
        addFactory { GetAllAnime(get()) }
        addFactory { GetAnimeBySource(get()) }
        addFactory { DeleteEpisodes(get()) }
        addFactory { DeleteAnimeById(get()) }
        addFactory { FilterSerializer() }
        addFactory { GetHistoryByAnimeId(get()) }
        addFactory { GetEpisodeByUrl(get()) }
        addFactory { GetSourceCategories(get()) }
        addFactory { CreateSourceCategory(get()) }
        addFactory { RenameSourceCategory(get(), get()) }
        addFactory { DeleteSourceCategory(get()) }
        addFactory { GetSortTag(get()) }
        addFactory { CreateSortTag(get(), get()) }
        addFactory { DeleteSortTag(get(), get()) }
        addFactory { ReorderSortTag(get(), get()) }
        addFactory { GetPagePreviews(get(), get()) }
        addFactory { SearchEngine() }
        addFactory { IsTrackUnfollowed() }
        addFactory { GetSeenAnimeNotInLibraryView(get()) }

        // Required for [MetadataSource]
        addFactory<MetadataSource.GetMangaId> { GetAnime(get()) }
        addFactory<MetadataSource.GetFlatMetadataById> { GetFlatMetadataById(get()) }
        addFactory<MetadataSource.InsertFlatMetadata> { InsertFlatMetadata(get()) }

        addSingletonFactory<AnimeMetadataRepository> { AnimeMetadataRepositoryImpl(get()) }
        addFactory { GetFlatMetadataById(get()) }
        addFactory { InsertFlatMetadata(get()) }
        addFactory { GetExhFavoriteAnimeWithMetadata(get()) }
        addFactory { GetSearchMetadata(get()) }
        addFactory { GetSearchTags(get()) }
        addFactory { GetSearchTitles(get()) }
        addFactory { GetIdsOfFavoriteAnimeWithMetadata(get()) }

        addSingletonFactory<AnimeMergeRepository> { AnimeMergeRepositoryImpl(get()) }
        addFactory { GetMergedAnime(get()) }
        addFactory { GetMergedAnimeById(get()) }
        addFactory { GetMergedReferencesById(get()) }
        addFactory { GetMergedEpisodesByAnimeId(get(), get()) }
        addFactory { InsertMergedReference(get()) }
        addFactory { UpdateMergedSettings(get()) }
        addFactory { DeleteByMergeId(get()) }
        addFactory { DeleteMergeById(get()) }
        addFactory { GetMergedAnimeForDownloading(get()) }
        // KMK -->
        addFactory { SmartSearchMerge(get()) }
        // KMK <--

        addSingletonFactory<FavoritesEntryRepository> { FavoritesEntryRepositoryImpl(get()) }
        addFactory { GetFavoriteEntries(get()) }
        addFactory { InsertFavoriteEntries(get()) }
        addFactory { DeleteFavoriteEntries(get()) }
        addFactory { InsertFavoriteEntryAlternative(get()) }

        addSingletonFactory<SavedSearchRepository> { SavedSearchRepositoryImpl(get()) }
        addFactory { GetSavedSearchById(get()) }
        addFactory { GetSavedSearchBySourceId(get()) }
        addFactory { DeleteSavedSearchById(get()) }
        addFactory { InsertSavedSearch(get()) }
        addFactory { GetExhSavedSearch(get(), get(), get()) }

        addSingletonFactory<FeedSavedSearchRepository> { FeedSavedSearchRepositoryImpl(get()) }
        addFactory { InsertFeedSavedSearch(get()) }
        addFactory { DeleteFeedSavedSearchById(get()) }
        addFactory { GetFeedSavedSearchGlobal(get()) }
        addFactory { GetFeedSavedSearchBySourceId(get()) }
        addFactory { CountFeedSavedSearchGlobal(get()) }
        addFactory { CountFeedSavedSearchBySourceId(get()) }
        addFactory { GetSavedSearchGlobalFeed(get()) }
        addFactory { GetSavedSearchBySourceIdFeed(get()) }
        // KMK -->
        addFactory { ReorderFeed(get()) }
        // KMK <--

        addSingletonFactory<CustomAnimeRepository> { CustomAnimeRepositoryImpl(get<Application>()) }
        addFactory { GetCustomAnimeInfo(get()) }
        addFactory { SetCustomAnimeInfo(get()) }
    }
}
