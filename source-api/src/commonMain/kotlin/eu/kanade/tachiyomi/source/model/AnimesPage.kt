package eu.kanade.tachiyomi.source.model

import exh.metadata.metadata.RaisedSearchMetadata

typealias AnimesPage = MangasPage
/* SY --> */
open /* SY <-- */ class MangasPage(open val animes: List<SAnime>, open val hasNextPage: Boolean) {
    // SY -->
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnimesPage) return false

        if (animes != other.animes) return false
        if (hasNextPage != other.hasNextPage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = animes.hashCode()
        result = 31 * result + hasNextPage.hashCode()
        return result
    }
    // SY <--

    fun copy(animes: List<SAnime> = this.animes, hasNextPage: Boolean = this.hasNextPage): AnimesPage {
        return AnimesPage(animes, hasNextPage)
    }

    override fun toString(): String {
        return "AnimesPage(animes=$animes, hasNextPage=$hasNextPage)"
    }
}

// SY -->
typealias MetadataAnimesPage = MetadataMangasPage
class MetadataMangasPage(
    override val animes: List<SAnime>,
    override val hasNextPage: Boolean,
    val animesMetadata: List<RaisedSearchMetadata>,
    val nextKey: Long? = null,
) : AnimesPage(animes, hasNextPage) {
    fun copy(
        animes: List<SAnime> = this.animes,
        hasNextPage: Boolean = this.hasNextPage,
        animesMetadata: List<RaisedSearchMetadata> = this.animesMetadata,
        nextKey: Long? = this.nextKey,
    ): AnimesPage {
        return MetadataAnimesPage(animes, hasNextPage, animesMetadata, nextKey)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as MetadataAnimesPage

        if (animes != other.animes) return false
        if (hasNextPage != other.hasNextPage) return false
        if (animesMetadata != other.animesMetadata) return false
        if (nextKey != other.nextKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + animes.hashCode()
        result = 31 * result + hasNextPage.hashCode()
        result = 31 * result + animesMetadata.hashCode()
        result = 31 * result + nextKey.hashCode()
        return result
    }

    override fun toString(): String {
        return "MetadataAnimesPage(" +
            "animes=$animes, " +
            "hasNextPage=$hasNextPage, " +
            "animesMetadata=$animesMetadata, " +
            "nextKey=$nextKey" +
            ")"
    }
}
// SY <--
