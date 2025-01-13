package eu.kanade.tachiyomi.data.track.animeupdates.dto

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.animeupdates.AnimeUpdates.Companion.READING_LIST
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MUListItem(
    val series: MUSeries? = null,
    @SerialName("list_id")
    val listId: Long? = null,
    val status: MUStatus? = null,
    val priority: Int? = null,
)

fun MUListItem.copyTo(track: Track): Track {
    return track.apply {
        this.status = listId ?: READING_LIST
        this.last_episode_seen = this@copyTo.status?.episode?.toDouble() ?: 0.0
    }
}
