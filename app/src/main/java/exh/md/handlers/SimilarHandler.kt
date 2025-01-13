package exh.md.handlers

import eu.kanade.tachiyomi.source.model.MetadataAnimesPage
import eu.kanade.tachiyomi.source.model.SAnime
import exh.md.dto.RelationListDto
import exh.md.dto.SimilarAnimeDto
import exh.md.service.MangaDexService
import exh.md.service.SimilarService
import exh.md.utils.MangaDexRelation
import exh.md.utils.MdUtil
import exh.metadata.metadata.MangaDexSearchMetadata
import tachiyomi.core.common.util.lang.withIOContext

class SimilarHandler(
    private val lang: String,
    private val service: MangaDexService,
    private val similarService: SimilarService,
) {

    suspend fun getSimilar(anime: SAnime): MetadataAnimesPage {
        val similarDto = withIOContext { similarService.getSimilarAnime(MdUtil.getAnimeId(anime.url)) }
        return similarDtoToAnimeListPage(similarDto)
    }

    private suspend fun similarDtoToAnimeListPage(
        similarAnimeDto: SimilarAnimeDto,
    ): MetadataAnimesPage {
        val ids = similarAnimeDto.matches.map {
            it.id
        }

        val animeList = service.viewAnimes(ids).data.map {
            MdUtil.createAnimeEntry(it, lang)
        }

        return MetadataAnimesPage(
            animeList, false,
            List(animeList.size) {
                MangaDexSearchMetadata().also { it.relation = MangaDexRelation.SIMILAR }
            },
        )
    }

    suspend fun getRelated(anime: SAnime): MetadataAnimesPage {
        val relatedListDto = withIOContext { service.relatedAnime(MdUtil.getAnimeId(anime.url)) }
        return relatedDtoToAnimeListPage(relatedListDto)
    }

    private suspend fun relatedDtoToAnimeListPage(
        relatedListDto: RelationListDto,
    ): MetadataAnimesPage {
        val ids = relatedListDto.data
            .mapNotNull { it.relationships.firstOrNull() }
            .map { it.id }

        val animeList = service.viewAnimes(ids).data.map {
            MdUtil.createAnimeEntry(it, lang)
        }

        return MetadataAnimesPage(
            animes = animeList,
            hasNextPage = false,
            animesMetadata = animeList.map { anime ->
                MangaDexSearchMetadata().also {
                    it.relation = relatedListDto.data
                        .firstOrNull { it.relationships.any { it.id == MdUtil.getAnimeId(anime.url) } }
                        ?.attributes?.relation?.let(MangaDexRelation::fromDex)
                }
            },
        )
    }
}
