package exh.md.service

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import exh.md.dto.SimilarAnimeDto
import exh.md.utils.MdUtil
import okhttp3.OkHttpClient

class SimilarService(
    private val client: OkHttpClient,
) {
    suspend fun getSimilarAnime(animeId: String): SimilarAnimeDto {
        return with(MdUtil.jsonParser) {
            client.newCall(
                GET(
                    "${MdUtil.similarBaseApi}$animeId.json",
                ),
            ).awaitSuccess().parseAs()
        }
    }
}
