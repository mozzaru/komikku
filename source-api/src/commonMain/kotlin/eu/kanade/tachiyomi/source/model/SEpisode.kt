@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.source.model

import java.io.Serializable

typealias SEpisode = SChapter
interface SChapter : Serializable {

    var url: String

    var name: String

    var date_upload: Long

    var episode_number: Float

    var scanlator: String?

    fun copyFrom(other: SEpisode) {
        name = other.name
        url = other.url
        date_upload = other.date_upload
        episode_number = other.episode_number
        scanlator = other.scanlator
    }

    companion object {
        fun create(): SEpisode {
            return SEpisodeImpl()
        }

        // SY -->
        operator fun invoke(
            name: String,
            url: String,
            date_upload: Long = 0,
            episode_number: Float = -1F,
            scanlator: String? = null,
        ): SEpisode {
            return create().apply {
                this.name = name
                this.url = url
                this.date_upload = date_upload
                this.episode_number = episode_number
                this.scanlator = scanlator
            }
        }
        // SY <--
    }
}
