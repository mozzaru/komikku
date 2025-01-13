package exh

import exh.source.BlacklistedSources
import exh.source.EH_SOURCE_ID
import exh.source.HBROWSE_OLD_ID
import exh.source.HBROWSE_SOURCE_ID
import exh.source.NHENTAI_OLD_ID
import exh.source.NHENTAI_SOURCE_ID
import exh.source.TSUMINO_OLD_ID
import exh.source.TSUMINO_SOURCE_ID
import tachiyomi.domain.anime.model.Anime
import java.net.URI
import java.net.URISyntaxException

object EXHMigrations {

    /**
     * Migrate old source ID of delegated sources in old backup
     */
    fun migrateBackupEntry(anime: Anime): Anime {
        var newAnime = anime
        if (newAnime.source == NHENTAI_OLD_ID) {
            newAnime = newAnime.copy(
                // Migrate the old source to the delegated one
                source = NHENTAI_SOURCE_ID,
                // Migrate nhentai URLs
                url = getUrlWithoutDomain(newAnime.url),
            )
        }

        // Migrate Tsumino source IDs
        if (newAnime.source == TSUMINO_OLD_ID) {
            newAnime = newAnime.copy(
                source = TSUMINO_SOURCE_ID,
            )
        }

        if (newAnime.source == HBROWSE_OLD_ID) {
            newAnime = newAnime.copy(
                source = HBROWSE_SOURCE_ID,
                url = newAnime.url + "/c00001/",
            )
        }

        // Allow importing of EHentai extension backups
        if (newAnime.source in BlacklistedSources.EHENTAI_EXT_SOURCES) {
            newAnime = newAnime.copy(
                source = EH_SOURCE_ID,
            )
        }

        return newAnime
    }

    private fun getUrlWithoutDomain(orig: String): String {
        return try {
            val uri = URI(orig)
            var out = uri.path
            if (uri.query != null) {
                out += "?" + uri.query
            }
            if (uri.fragment != null) {
                out += "#" + uri.fragment
            }
            out
        } catch (e: URISyntaxException) {
            orig
        }
    }
}
