package tachiyomi.data.anime

import exh.metadata.metadata.base.FlatMetadata
import exh.metadata.sql.models.SearchMetadata
import exh.metadata.sql.models.SearchTag
import exh.metadata.sql.models.SearchTitle
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.repository.AnimeMetadataRepository

class AnimeMetadataRepositoryImpl(
    private val handler: DatabaseHandler,
) : AnimeMetadataRepository {

    override suspend fun getMetadataById(id: Long): SearchMetadata? {
        return handler.awaitOneOrNull { search_metadataQueries.selectByAnimeId(id, ::searchMetadataMapper) }
    }

    override fun subscribeMetadataById(id: Long): Flow<SearchMetadata?> {
        return handler.subscribeToOneOrNull { search_metadataQueries.selectByAnimeId(id, ::searchMetadataMapper) }
    }

    override suspend fun getTagsById(id: Long): List<SearchTag> {
        return handler.awaitList { search_tagsQueries.selectByAnimeId(id, ::searchTagMapper) }
    }

    override fun subscribeTagsById(id: Long): Flow<List<SearchTag>> {
        return handler.subscribeToList { search_tagsQueries.selectByAnimeId(id, ::searchTagMapper) }
    }

    override suspend fun getTitlesById(id: Long): List<SearchTitle> {
        return handler.awaitList { search_titlesQueries.selectByAnimeId(id, ::searchTitleMapper) }
    }

    override fun subscribeTitlesById(id: Long): Flow<List<SearchTitle>> {
        return handler.subscribeToList { search_titlesQueries.selectByAnimeId(id, ::searchTitleMapper) }
    }

    override suspend fun insertFlatMetadata(flatMetadata: FlatMetadata) {
        require(flatMetadata.metadata.animeId != -1L)

        handler.await(true) {
            flatMetadata.metadata.run {
                search_metadataQueries.upsert(animeId, uploader, extra, indexedExtra, extraVersion.toLong())
            }
            search_tagsQueries.deleteByAnime(flatMetadata.metadata.animeId)
            flatMetadata.tags.forEach {
                search_tagsQueries.insert(it.animeId, it.namespace, it.name, it.type.toLong())
            }
            search_titlesQueries.deleteByAnime(flatMetadata.metadata.animeId)
            flatMetadata.titles.forEach {
                search_titlesQueries.insert(it.animeId, it.title, it.type.toLong())
            }
        }
    }

    override suspend fun getExhFavoriteAnimeWithMetadata(): List<Anime> {
        return handler.awaitList {
            animesQueries.getEhAnimeWithMetadata(EH_SOURCE_ID, EXH_SOURCE_ID, AnimeMapper::mapAnime)
        }
    }

    override suspend fun getIdsOfFavoriteAnimeWithMetadata(): List<Long> {
        return handler.awaitList { animesQueries.getIdsOfFavoriteAnimeWithMetadata() }
    }

    override suspend fun getSearchMetadata(): List<SearchMetadata> {
        return handler.awaitList { search_metadataQueries.selectAll(::searchMetadataMapper) }
    }

    private fun searchMetadataMapper(
        animeId: Long,
        uploader: String?,
        extra: String,
        indexedExtra: String?,
        extraVersion: Long,
    ): SearchMetadata {
        return SearchMetadata(
            animeId = animeId,
            uploader = uploader,
            extra = extra,
            indexedExtra = indexedExtra,
            extraVersion = extraVersion.toInt(),
        )
    }

    private fun searchTitleMapper(
        animeId: Long,
        id: Long?,
        title: String,
        type: Long,
    ): SearchTitle {
        return SearchTitle(
            animeId = animeId,
            id = id,
            title = title,
            type = type.toInt(),
        )
    }

    private fun searchTagMapper(
        animeId: Long,
        id: Long?,
        namespace: String?,
        name: String,
        type: Long,
    ): SearchTag {
        return SearchTag(
            animeId = animeId,
            id = id,
            namespace = namespace,
            name = name,
            type = type.toInt(),
        )
    }
}
