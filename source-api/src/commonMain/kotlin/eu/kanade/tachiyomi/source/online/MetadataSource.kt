package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.base.FlatMetadata
import rx.Completable
import rx.Single
import tachiyomi.core.common.util.lang.awaitSingle
import tachiyomi.core.common.util.lang.runAsObservable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.reflect.KClass

/**
 * LEWD!
 */
interface MetadataSource<M : RaisedSearchMetadata, I> : CatalogueSource {
    interface GetAnimeId {
        suspend fun awaitId(url: String, sourceId: Long): Long?
    }
    interface InsertFlatMetadata {
        suspend fun await(metadata: RaisedSearchMetadata)
    }
    interface GetFlatMetadataById {
        suspend fun await(id: Long): FlatMetadata?
    }
    val getAnimeId: GetAnimeId get() = Injekt.get()
    val insertFlatMetadata: InsertFlatMetadata get() = Injekt.get()
    val getFlatMetadataById: GetFlatMetadataById get() = Injekt.get()

    /**
     * The class of the metadata used by this source
     */
    val metaClass: KClass<M>

    /**
     * Parse the supplied input into the supplied metadata object
     */
    suspend fun parseIntoMetadata(metadata: M, input: I)

    /**
     * Use reflection to create a new instance of metadata
     */
    fun newMetaInstance(): M

    /**
     * Parses metadata from the input and then copies it into the anime
     *
     * Will also save the metadata to the DB if possible
     */
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Use the AnimeInfo variant")
    fun parseToAnimeCompletable(anime: SAnime, input: I): Completable = runAsObservable {
        parseToAnime(anime, input)
    }.toCompletable()

    suspend fun parseToAnime(anime: SAnime, input: I): SAnime {
        val animeId = anime.id()
        val metadata = if (animeId != null) {
            val flatMetadata = getFlatMetadataById.await(animeId)
            flatMetadata?.raise(metaClass) ?: newMetaInstance()
        } else {
            newMetaInstance()
        }

        parseIntoMetadata(metadata, input)
        if (animeId != null) {
            metadata.animeId = animeId
            insertFlatMetadata.await(metadata)
        }

        return metadata.createAnimeInfo(anime)
    }

    /**
     * Try to first get the metadata from the DB. If the metadata is not in the DB, calls the input
     * producer and parses the metadata from the input
     *
     * If the metadata needs to be parsed from the input producer, the resulting parsed metadata will
     * also be saved to the DB.
     */
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("use fetchOrLoadMetadata made for AnimeInfo")
    fun getOrLoadMetadata(animeId: Long?, inputProducer: () -> Single<I>): Single<M> =
        runAsObservable {
            fetchOrLoadMetadata(animeId) { inputProducer().toObservable().awaitSingle() }
        }.toSingle()

    /**
     * Try to first get the metadata from the DB. If the metadata is not in the DB, calls the input
     * producer and parses the metadata from the input
     *
     * If the metadata needs to be parsed from the input producer, the resulting parsed metadata will
     * also be saved to the DB.
     */
    suspend fun fetchOrLoadMetadata(animeId: Long?, inputProducer: suspend () -> I): M {
        val meta = if (animeId != null) {
            val flatMetadata = getFlatMetadataById.await(animeId)
            flatMetadata?.raise(metaClass)
        } else {
            null
        }

        return meta ?: inputProducer().let { input ->
            val newMeta = newMetaInstance()
            parseIntoMetadata(newMeta, input)
            if (animeId != null) {
                newMeta.animeId = animeId
                insertFlatMetadata.await(newMeta)
            }
            newMeta
        }
    }

    suspend fun SAnime.id() = getAnimeId.awaitId(url, id)
}
