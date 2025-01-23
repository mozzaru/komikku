package exh.md.similar

import eu.kanade.domain.anime.model.toSAnime
import eu.kanade.tachiyomi.source.model.AnimesPage
import eu.kanade.tachiyomi.source.model.MetadataAnimesPage
import eu.kanade.tachiyomi.source.online.all.MangaDex
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import tachiyomi.data.source.NoResultsException
import tachiyomi.data.source.SourcePagingSource
import tachiyomi.domain.anime.model.Anime

/**
 * MangaDexSimilarPagingSource inherited from the general Pager.
 */
class MangaDexSimilarPagingSource(val anime: Anime, val mangadex: MangaDex) : SourcePagingSource(mangadex) {

    override suspend fun requestNextPage(currentPage: Int): AnimesPage {
        val animesPage = coroutineScope {
            val similarPageDef = async { mangadex.getMangaSimilar(anime.toSAnime()) }
            val relatedPageDef = async { mangadex.getMangaRelated(anime.toSAnime()) }
            val similarPage = similarPageDef.await()
            val relatedPage = relatedPageDef.await()

            MetadataAnimesPage(
                relatedPage.animes + similarPage.animes,
                false,
                relatedPage.animesMetadata + similarPage.animesMetadata,
            )
        }

        return animesPage.takeIf { it.animes.isNotEmpty() } ?: throw NoResultsException()
    }
}
