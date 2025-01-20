package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import eu.kanade.presentation.library.components.AnimeCompactGridItem
import eu.kanade.presentation.library.components.CommonAnimeItemDefaults
import eu.kanade.tachiyomi.R
import exh.metadata.metadata.MangaDexSearchMetadata
import exh.metadata.metadata.RaisedSearchMetadata
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.anime.model.AnimeCover
import tachiyomi.domain.anime.model.Anime
import tachiyomi.presentation.core.components.Badge
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus

@Composable
fun BrowseSourceCompactGrid(
    animeList: LazyPagingItems<StateFlow</* SY --> */Pair<Anime, RaisedSearchMetadata?>/* SY <-- */>>,
    columns: GridCells,
    contentPadding: PaddingValues,
    onMangaClick: (Anime) -> Unit,
    onMangaLongClick: (Anime) -> Unit,
    // KMK -->
    selection: List<Anime>,
    // KMK <--
) {
    LazyVerticalGrid(
        columns = columns,
        contentPadding = contentPadding + PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(CommonAnimeItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonAnimeItemDefaults.GridHorizontalSpacer),
    ) {
        if (animeList.loadState.prepend is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }

        items(count = animeList.itemCount) { index ->
            // SY -->
            val pair by animeList[index]?.collectAsState() ?: return@items
            val manga = pair.first
            val metadata = pair.second
            // SY <--

            BrowseSourceCompactGridItem(
                anime = manga,
                // SY -->
                metadata = metadata,
                // SY <--
                onClick = { onMangaClick(manga) },
                onLongClick = { onMangaLongClick(manga) },
                // KMK -->
                isSelected = selection.fastAny { selected -> selected.id == manga.id },
                // KMK <--
            )
        }

        if (animeList.loadState.refresh is LoadState.Loading || animeList.loadState.append is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseSourceLoadingItem()
            }
        }
    }
}

@Composable
internal fun BrowseSourceCompactGridItem(
    anime: Anime,
    // SY -->
    metadata: RaisedSearchMetadata?,
    // SY <--
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = onClick,
    // KMK -->
    isSelected: Boolean = false,
    // KMK <--
) {
    AnimeCompactGridItem(
        title = anime.title,
        coverData = AnimeCover(
            animeId = anime.id,
            sourceId = anime.source,
            isAnimeFavorite = anime.favorite,
            ogUrl = anime.thumbnailUrl,
            lastModified = anime.coverLastModified,
        ),
        // KMK -->
        isSelected = isSelected,
        // KMK <--
        coverAlpha = if (anime.favorite) CommonAnimeItemDefaults.BrowseFavoriteCoverAlpha else 1f,
        coverBadgeStart = {
            InLibraryBadge(enabled = anime.favorite)
        },
        // SY -->
        coverBadgeEnd = {
            if (metadata is MangaDexSearchMetadata) {
                metadata.followStatus?.let { followStatus ->
                    val text = LocalContext.current
                        .resources
                        .let {
                            remember {
                                it.getStringArray(R.array.md_follows_options)
                                    .getOrNull(followStatus)
                            }
                        }
                        ?: return@let
                    Badge(
                        text = text,
                        color = MaterialTheme.colorScheme.tertiary,
                        textColor = MaterialTheme.colorScheme.onTertiary,
                    )
                }
                metadata.relation?.let {
                    Badge(
                        text = stringResource(it.res),
                        color = MaterialTheme.colorScheme.tertiary,
                        textColor = MaterialTheme.colorScheme.onTertiary,
                    )
                }
            }
        },
        // SY <--
        onLongClick = onLongClick,
        onClick = onClick,
    )
}
