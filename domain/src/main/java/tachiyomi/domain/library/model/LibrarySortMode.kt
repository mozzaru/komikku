package tachiyomi.domain.library.model

import tachiyomi.domain.category.model.Category

data class LibrarySort(
    val type: Type,
    val direction: Direction,
) : FlagWithMask {

    override val flag: Long
        get() = type + direction

    override val mask: Long
        get() = type.mask or direction.mask

    val isAscending: Boolean
        get() = direction == Direction.Ascending

    sealed class Type(
        override val flag: Long,
    ) : FlagWithMask {

        override val mask: Long = 0b00111100L

        data object Alphabetical : Type(0b00000000)
        data object LastSeen : Type(0b00000100)
        data object LastUpdate : Type(0b00001000)
        data object UnseenCount : Type(0b00001100)
        data object TotalEpisodes : Type(0b00010000)
        data object LatestEpisode : Type(0b00010100)
        data object EpisodeFetchDate : Type(0b00011000)
        data object DateAdded : Type(0b00011100)
        data object TrackerMean : Type(0b00100000)
        data object Random : Type(0b00111100)

        // SY -->
        data object TagList : Type(0b00100100)
        // SY <--

        companion object {
            fun valueOf(flag: Long): Type {
                return types.find { type -> type.flag == flag and type.mask } ?: default.type
            }
        }
    }

    sealed class Direction(
        override val flag: Long,
    ) : FlagWithMask {

        override val mask: Long = 0b01000000L

        data object Ascending : Direction(0b01000000)
        data object Descending : Direction(0b00000000)

        companion object {
            fun valueOf(flag: Long): Direction {
                return directions.find { direction -> direction.flag == flag and direction.mask } ?: default.direction
            }
        }
    }

    object Serializer {
        fun deserialize(serialized: String): LibrarySort {
            return LibrarySort.deserialize(serialized)
        }

        fun serialize(value: LibrarySort): String {
            return value.serialize()
        }
    }

    companion object {
        val types by lazy {
            setOf(
                Type.Alphabetical,
                Type.LastSeen,
                Type.LastUpdate,
                Type.UnseenCount,
                Type.TotalEpisodes,
                Type.LatestEpisode,
                Type.EpisodeFetchDate,
                Type.DateAdded,
                Type.TrackerMean,
                /* SY -->*/ Type.TagList, /* SY <--*/
                Type.Random,
            )
        }
        val directions by lazy { setOf(Direction.Ascending, Direction.Descending) }
        val default = LibrarySort(Type.Alphabetical, Direction.Ascending)

        fun valueOf(flag: Long?): LibrarySort {
            if (flag == null) return default
            return LibrarySort(
                Type.valueOf(flag),
                Direction.valueOf(flag),
            )
        }

        fun deserialize(serialized: String): LibrarySort {
            if (serialized.isEmpty()) return default
            return try {
                val values = serialized.split(",")
                val type = when (values[0]) {
                    "ALPHABETICAL" -> Type.Alphabetical
                    "LAST_READ" -> Type.LastSeen
                    "LAST_MANGA_UPDATE" -> Type.LastUpdate
                    "UNREAD_COUNT" -> Type.UnseenCount
                    "TOTAL_CHAPTERS" -> Type.TotalEpisodes
                    "LATEST_CHAPTER" -> Type.LatestEpisode
                    "CHAPTER_FETCH_DATE" -> Type.EpisodeFetchDate
                    "DATE_ADDED" -> Type.DateAdded
                    "TRACKER_MEAN" -> Type.TrackerMean
                    // SY -->
                    "TAG_LIST" -> Type.TagList
                    // SY <--
                    "RANDOM" -> Type.Random
                    else -> Type.Alphabetical
                }
                val ascending = if (values[1] == "ASCENDING") Direction.Ascending else Direction.Descending
                LibrarySort(type, ascending)
            } catch (e: Exception) {
                default
            }
        }
    }

    fun serialize(): String {
        val type = when (type) {
            Type.Alphabetical -> "ALPHABETICAL"
            Type.LastSeen -> "LAST_READ"
            Type.LastUpdate -> "LAST_MANGA_UPDATE"
            Type.UnseenCount -> "UNREAD_COUNT"
            Type.TotalEpisodes -> "TOTAL_CHAPTERS"
            Type.LatestEpisode -> "LATEST_CHAPTER"
            Type.EpisodeFetchDate -> "CHAPTER_FETCH_DATE"
            Type.DateAdded -> "DATE_ADDED"
            Type.TrackerMean -> "TRACKER_MEAN"
            // SY -->
            Type.TagList -> "TAG_LIST"
            // SY <--
            Type.Random -> "RANDOM"
        }
        val direction = if (direction == Direction.Ascending) "ASCENDING" else "DESCENDING"
        return "$type,$direction"
    }
}

val Category?.sort: LibrarySort
    get() = LibrarySort.valueOf(this?.flags)
