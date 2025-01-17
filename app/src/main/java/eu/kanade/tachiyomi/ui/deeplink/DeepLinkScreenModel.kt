package eu.kanade.tachiyomi.ui.deeplink

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.anime.model.toDomainManga
import eu.kanade.domain.anime.model.toSManga
import eu.kanade.domain.episode.interactor.SyncEpisodesWithSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.online.ResolvableAnimeSource
import eu.kanade.tachiyomi.animesource.online.UriType
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.anime.interactor.GetAnimeByUrlAndSourceId
import tachiyomi.domain.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.episode.interactor.GetEpisodeByUrlAndAnimeId
import tachiyomi.domain.episode.model.Episode
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
                .filterIsInstance<ResolvableAnimeSource>()
                .firstOrNull { it.getUriType(query) != UriType.Unknown }

            val manga = source?.getAnime(query)?.let {
                getMangaFromSManga(it, source.id)
            }

            val chapter = if (source?.getUriType(query) == UriType.Episode && manga != null) {
                source.getEpisode(query)?.let { getChapterFromSChapter(it, manga, source) }
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

    private suspend fun getChapterFromSChapter(sEpisode: SEpisode, manga: Manga, source: AnimeSource): Episode? {
        val localChapter = getEpisodeByUrlAndAnimeId.await(sEpisode.url, manga.id)

        return if (localChapter == null) {
            val sourceChapters = source.getEpisodeList(manga.toSManga())
            val newChapters = syncEpisodesWithSource.await(sourceChapters, manga, source, false)
            newChapters.find { it.url == sEpisode.url }
        } else {
            localChapter
        }
    }

    private suspend fun getMangaFromSManga(sAnime: SAnime, sourceId: Long): Manga {
        return getAnimeByUrlAndSourceId.await(sAnime.url, sourceId)
            ?: networkToLocalAnime.await(sAnime.toDomainManga(sourceId))
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
