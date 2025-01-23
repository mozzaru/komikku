package exh.md.similar

import eu.kanade.domain.anime.model.toSAnime
import eu.kanade.tachiyomi.source.model.MangasPage
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
class MangaDexSimilarPagingSource(val manga: Anime, val mangadex: MangaDex) : SourcePagingSource(mangadex) {

    override suspend fun requestNextPage(currentPage: Int): MangasPage {
        val mangasPage = coroutineScope {
            val similarPageDef = async { mangadex.getMangaSimilar(manga.toSAnime()) }
            val relatedPageDef = async { mangadex.getMangaRelated(manga.toSAnime()) }
            val similarPage = similarPageDef.await()
            val relatedPage = relatedPageDef.await()

            MetadataAnimesPage(
                relatedPage.animes + similarPage.animes,
                false,
                relatedPage.animesMetadata + similarPage.animesMetadata,
            )
        }

        return mangasPage.takeIf { it.animes.isNotEmpty() } ?: throw NoResultsException()
    }
}
