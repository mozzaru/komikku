package exh.pagepreview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.anime.interactor.GetPagePreviews
import eu.kanade.domain.anime.model.PagePreview
import eu.kanade.tachiyomi.animesource.AnimeSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class PagePreviewScreenModel(
    private val mangaId: Long,
    private val getPagePreviews: GetPagePreviews = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val getEpisodesByAnimeId: GetEpisodesByAnimeId = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
) : StateScreenModel<PagePreviewState>(PagePreviewState.Loading) {

    private val page = MutableStateFlow(1)

    var pageDialogOpen by mutableStateOf(false)

    init {
        screenModelScope.launchIO {
            val manga = getAnime.await(mangaId)!!
            val chapter = getEpisodesByAnimeId.await(mangaId).minByOrNull { it.sourceOrder }
            if (chapter == null) {
                mutableState.update {
                    PagePreviewState.Error(Exception("No episodes found"))
                }
                return@launchIO
            }
            val source = sourceManager.getOrStub(manga.source)
            page
                .onEach { page ->
                    when (
                        val previews = getPagePreviews.await(manga, source, page)
                    ) {
                        is GetPagePreviews.Result.Error -> mutableState.update {
                            PagePreviewState.Error(previews.error)
                        }
                        is GetPagePreviews.Result.Success -> mutableState.update {
                            when (it) {
                                PagePreviewState.Loading, is PagePreviewState.Error -> {
                                    PagePreviewState.Success(
                                        page,
                                        previews.pagePreviews,
                                        previews.hasNextPage,
                                        previews.pageCount,
                                        manga,
                                        chapter,
                                        source,
                                    )
                                }
                                is PagePreviewState.Success -> it.copy(
                                    page = page,
                                    pagePreviews = previews.pagePreviews,
                                    hasNextPage = previews.hasNextPage,
                                    pageCount = previews.pageCount,
                                )
                            }
                        }
                        GetPagePreviews.Result.Unused -> Unit
                    }
                }
                .catch { e ->
                    mutableState.update {
                        PagePreviewState.Error(e)
                    }
                }
                .collect()
        }
    }

    fun moveToPage(page: Int) {
        this.page.value = page
    }
}

sealed class PagePreviewState {
    data object Loading : PagePreviewState()

    data class Success(
        val page: Int,
        val pagePreviews: List<PagePreview>,
        val hasNextPage: Boolean,
        val pageCount: Int?,
        val anime: Anime,
        val episode: Episode,
        val source: AnimeSource,
    ) : PagePreviewState()

    data class Error(val error: Throwable) : PagePreviewState()
}
