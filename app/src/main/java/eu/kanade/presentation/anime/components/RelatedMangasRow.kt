package eu.kanade.presentation.anime.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.components.AnimeItem
import eu.kanade.presentation.browse.components.EmptyResultItem
import eu.kanade.presentation.browse.components.GlobalSearchLoadingResultItem
import eu.kanade.tachiyomi.ui.anime.RelatedAnime
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.asAnimeCover
import tachiyomi.presentation.core.components.material.padding

@Composable
fun RelatedAnimesRow(
    relatedAnimes: List<RelatedAnime>?,
    getAnimeState: @Composable (Anime) -> State<Anime>,
    onAnimeClick: (Anime) -> Unit,
    onAnimeLongClick: (Anime) -> Unit,
) {
    when {
        relatedAnimes == null -> {
            GlobalSearchLoadingResultItem()
        }

        relatedAnimes.isNotEmpty() -> {
            RelatedAnimeCardRow(
                relatedAnimes = relatedAnimes,
                getAnime = { getAnimeState(it) },
                onAnimeClick = onAnimeClick,
                onAnimeLongClick = onAnimeLongClick,
            )
        }

        else -> {
            EmptyResultItem()
        }
    }
}

@Composable
fun RelatedAnimeCardRow(
    relatedAnimes: List<RelatedAnime>,
    getAnime: @Composable (Anime) -> State<Anime>,
    onAnimeClick: (Anime) -> Unit,
    onAnimeLongClick: (Anime) -> Unit,
) {
    val animes = relatedAnimes.filterIsInstance<RelatedAnime.Success>().map { it.animeList }.flatten()
    val loading = relatedAnimes.filterIsInstance<RelatedAnime.Loading>().firstOrNull()

    LazyRow(
        contentPadding = PaddingValues(MaterialTheme.padding.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        items(animes, key = { "related-row-${it.id}" }) {
            val anime by getAnime(it)
            AnimeItem(
                title = anime.title,
                cover = anime.asAnimeCover(),
                isFavorite = anime.favorite,
                onClick = { onAnimeClick(anime) },
                onLongClick = { onAnimeLongClick(anime) },
                isSelected = false,
            )
        }
        if (loading != null) {
            item {
                RelatedAnimeLoadingItem()
            }
        }
    }
}

@Composable
fun RelatedAnimeLoadingItem() {
    Box(
        modifier = Modifier
            .width(96.dp)
            .aspectRatio(AnimeCover.Book.ratio)
            .padding(vertical = MaterialTheme.padding.medium),
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.Center),
            strokeWidth = 2.dp,
        )
    }
}
