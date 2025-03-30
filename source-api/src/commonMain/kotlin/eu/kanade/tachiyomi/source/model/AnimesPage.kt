package eu.kanade.tachiyomi.source.model

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

    fun copy(animes: List<SAnime> = this.animes, hasNextPage: Boolean = this.hasNextPage): AnimesPage {
        return AnimesPage(animes, hasNextPage)
    }

    override fun toString(): String {
        return "AnimesPage(animes=$animes, hasNextPage=$hasNextPage)"
    }
}
