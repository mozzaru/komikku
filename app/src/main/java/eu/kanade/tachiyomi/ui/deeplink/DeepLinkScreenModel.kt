package eu.kanade.tachiyomi.ui.deeplink

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.anime.model.toDomainAnime
import eu.kanade.domain.anime.model.toSAnime
import eu.kanade.domain.episode.interactor.SyncEpisodesWithSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.online.ResolvableSource
import eu.kanade.tachiyomi.source.online.UriType
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.manga.interactor.GetAnimeByUrlAndSourceId
import tachiyomi.domain.manga.interactor.NetworkToLocalAnime
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.chapter.interactor.GetEpisodeByUrlAndAnimeId
import tachiyomi.domain.chapter.model.Episode
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DeepLinkScreenModel(
    query: String = "",
    private val sourceManager: SourceManager = Injekt.get(),
    private val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
    private val getEpisodeByUrlAndAnimeId: GetEpisodeByUrlAndAnimeId = Injekt.get(),
    private val getAnimeByUrlAndSourceId: GetAnimeByUrlAndSourceId = Injekt.get(),
    private val syncEpisodesWithSource: SyncEpisodesWithSource = Injekt.get(),
) : StateScreenModel<DeepLinkScreenModel.State>(State.Loading) {

    init {
        screenModelScope.launchIO {
            val source = sourceManager.getCatalogueSources()
                .filterIsInstance<ResolvableSource>()
                .firstOrNull { it.getUriType(query) != UriType.Unknown }

            val manga = source?.getManga(query)?.let {
                getMangaFromSManga(it, source.id)
            }

            val chapter = if (source?.getUriType(query) == UriType.Chapter && manga != null) {
                source.getChapter(query)?.let { getEpisodeFromSEpisode(it, manga, source) }
            } else {
                null
            }

            mutableState.update {
                if (manga == null) {
                    State.NoResults
                } else {
                    if (chapter == null) {
                        State.Result(manga)
                    } else {
                        State.Result(manga, chapter.id)
                    }
                }
            }
        }
    }

    private suspend fun getEpisodeFromSEpisode(sChapter: SChapter, manga: Manga, source: Source): Episode? {
        val localChapter = getEpisodeByUrlAndAnimeId.await(sChapter.url, manga.id)

        return if (localChapter == null) {
            val sourceChapters = source.getChapterList(manga.toSAnime())
            val newChapters = syncEpisodesWithSource.await(sourceChapters, manga, source, false)
            newChapters.find { it.url == sChapter.url }
        } else {
            localChapter
        }
    }

    private suspend fun getMangaFromSManga(sManga: SManga, sourceId: Long): Manga {
        return getAnimeByUrlAndSourceId.await(sManga.url, sourceId)
            ?: networkToLocalAnime.await(sManga.toDomainAnime(sourceId))
    }

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data object NoResults : State

        @Immutable
        data class Result(val manga: Manga, val chapterId: Long? = null) : State
    }
}
