package eu.kanade.tachiyomi.ui.reader.model

sealed class EpisodeTransition : ReaderItem {

    abstract val from: ReaderEpisode
    abstract val to: ReaderEpisode?

    class Prev(
        override val from: ReaderEpisode,
        override val to: ReaderEpisode?,
    ) : EpisodeTransition()

    class Next(
        override val from: ReaderEpisode,
        override val to: ReaderEpisode?,
    ) : EpisodeTransition()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EpisodeTransition) return false
        if (from == other.from && to == other.to) return true
        if (from == other.to && to == other.from) return true
        return false
    }

    override fun hashCode(): Int {
        var result = from.hashCode()
        result = 31 * result + (to?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "${javaClass.simpleName}(from=${from.episode.url}, to=${to?.episode?.url})"
    }
}
