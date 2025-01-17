package eu.kanade.tachiyomi.animesource.model

import exh.metadata.metadata.RaisedSearchMetadata

/* SY --> */
open /* SY <-- */ class AnimesPage(open val animes: List<SAnime>, open val hasNextPage: Boolean) {
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

    fun copy(mangas: List<SAnime> = this.animes, hasNextPage: Boolean = this.hasNextPage): AnimesPage {
        return AnimesPage(mangas, hasNextPage)
    }

    override fun toString(): String {
        return "AnimesPage(mangas=$animes, hasNextPage=$hasNextPage)"
    }
}

// SY -->
class MetadataAnimesPage(
    override val animes: List<SAnime>,
    override val hasNextPage: Boolean,
    val mangasMetadata: List<RaisedSearchMetadata>,
    val nextKey: Long? = null,
) : AnimesPage(animes, hasNextPage) {
    fun copy(
        mangas: List<SAnime> = this.animes,
        hasNextPage: Boolean = this.hasNextPage,
        mangasMetadata: List<RaisedSearchMetadata> = this.mangasMetadata,
        nextKey: Long? = this.nextKey,
    ): AnimesPage {
        return MetadataAnimesPage(mangas, hasNextPage, mangasMetadata, nextKey)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as MetadataAnimesPage

        if (animes != other.animes) return false
        if (hasNextPage != other.hasNextPage) return false
        if (mangasMetadata != other.mangasMetadata) return false
        if (nextKey != other.nextKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + animes.hashCode()
        result = 31 * result + hasNextPage.hashCode()
        result = 31 * result + mangasMetadata.hashCode()
        result = 31 * result + nextKey.hashCode()
        return result
    }

    override fun toString(): String {
        return "MetadataAnimesPage(" +
            "mangas=$animes, " +
            "hasNextPage=$hasNextPage, " +
            "mangasMetadata=$mangasMetadata, " +
            "nextKey=$nextKey" +
            ")"
    }
}
// SY <--
