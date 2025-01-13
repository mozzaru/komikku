package eu.kanade.tachiyomi.data.track.animeupdates

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.animeupdates.AnimeUpdates.Companion.READING_LIST
import eu.kanade.tachiyomi.data.track.animeupdates.AnimeUpdates.Companion.WISH_LIST
import eu.kanade.tachiyomi.data.track.animeupdates.dto.MUContext
import eu.kanade.tachiyomi.data.track.animeupdates.dto.MUListItem
import eu.kanade.tachiyomi.data.track.animeupdates.dto.MULoginResponse
import eu.kanade.tachiyomi.data.track.animeupdates.dto.MURating
import eu.kanade.tachiyomi.data.track.animeupdates.dto.MURecord
import eu.kanade.tachiyomi.data.track.animeupdates.dto.MUSearchResult
import eu.kanade.tachiyomi.network.DELETE
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.PUT
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import uy.kohesive.injekt.injectLazy
import tachiyomi.domain.track.model.Track as DomainTrack

class AnimeUpdatesApi(
    interceptor: AnimeUpdatesInterceptor,
    private val client: OkHttpClient,
) {
    private val json: Json by injectLazy()

    private val authClient by lazy {
        client.newBuilder()
            .addInterceptor(interceptor)
            .build()
    }

    suspend fun getSeriesListItem(track: Track): Pair<MUListItem, MURating?> {
        val listItem = with(json) {
            authClient.newCall(GET("$BASE_URL/v1/lists/series/${track.remote_id}"))
                .awaitSuccess()
                .parseAs<MUListItem>()
        }

        val rating = getSeriesRating(track)

        return listItem to rating
    }

    suspend fun addSeriesToList(track: Track, hasReadEpisodes: Boolean) {
        val status = if (hasReadEpisodes) READING_LIST else WISH_LIST
        val body = buildJsonArray {
            addJsonObject {
                putJsonObject("series") {
                    put("id", track.remote_id)
                }
                put("list_id", status)
            }
        }
        authClient.newCall(
            POST(
                url = "$BASE_URL/v1/lists/series",
                body = body.toString().toRequestBody(CONTENT_TYPE),
            ),
        )
            .awaitSuccess()
            .let {
                if (it.code == 200) {
                    track.status = status
                    track.last_episode_seen = 1.0
                }
            }
    }

    suspend fun updateSeriesListItem(track: Track) {
        val body = buildJsonArray {
            addJsonObject {
                putJsonObject("series") {
                    put("id", track.remote_id)
                }
                put("list_id", track.status)
                putJsonObject("status") {
                    put("episode", track.last_episode_seen.toInt())
                }
            }
        }
        authClient.newCall(
            POST(
                url = "$BASE_URL/v1/lists/series/update",
                body = body.toString().toRequestBody(CONTENT_TYPE),
            ),
        )
            .awaitSuccess()

        updateSeriesRating(track)
    }

    suspend fun deleteSeriesFromList(track: DomainTrack) {
        val body = buildJsonArray {
            add(track.remoteId)
        }
        authClient.newCall(
            POST(
                url = "$BASE_URL/v1/lists/series/delete",
                body = body.toString().toRequestBody(CONTENT_TYPE),
            ),
        )
            .awaitSuccess()
    }

    private suspend fun getSeriesRating(track: Track): MURating? {
        return try {
            with(json) {
                authClient.newCall(GET("$BASE_URL/v1/series/${track.remote_id}/rating"))
                    .awaitSuccess()
                    .parseAs<MURating>()
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun updateSeriesRating(track: Track) {
        if (track.score < 0.0) return
        if (track.score != 0.0) {
            val body = buildJsonObject {
                put("rating", track.score)
            }
            authClient.newCall(
                PUT(
                    url = "$BASE_URL/v1/series/${track.remote_id}/rating",
                    body = body.toString().toRequestBody(CONTENT_TYPE),
                ),
            )
                .awaitSuccess()
        } else {
            authClient.newCall(
                DELETE(url = "$BASE_URL/v1/series/${track.remote_id}/rating"),
            )
                .awaitSuccess()
        }
    }

    suspend fun search(query: String): List<MURecord> {
        val body = buildJsonObject {
            put("search", query)
            put(
                "filter_types",
                buildJsonArray {
                    add("drama cd")
                    add("novel")
                },
            )
        }

        return with(json) {
            client.newCall(
                POST(
                    url = "$BASE_URL/v1/series/search",
                    body = body.toString().toRequestBody(CONTENT_TYPE),
                ),
            )
                .awaitSuccess()
                .parseAs<MUSearchResult>()
                .results
                .map { it.record }
        }
    }

    suspend fun authenticate(username: String, password: String): MUContext? {
        val body = buildJsonObject {
            put("username", username)
            put("password", password)
        }
        return with(json) {
            client.newCall(
                PUT(
                    url = "$BASE_URL/v1/account/login",
                    body = body.toString().toRequestBody(CONTENT_TYPE),
                ),
            )
                .awaitSuccess()
                .parseAs<MULoginResponse>()
                .context
        }
    }

    suspend fun getSeries(track: DomainTrack): MURecord {
        return with(json) {
            client.newCall(GET("$BASE_URL/v1/series/${track.remoteId}"))
                .awaitSuccess()
                .parseAs<MURecord>()
        }
    }

    companion object {
        private const val BASE_URL = "https://api.animeupdates.com"

        private val CONTENT_TYPE = "application/vnd.api+json".toMediaType()
    }
}
