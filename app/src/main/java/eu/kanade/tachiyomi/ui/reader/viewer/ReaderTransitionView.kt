package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.reader.EpisodeTransition
import eu.kanade.presentation.theme.TachiyomiTheme
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.reader.model.EpisodeTransition
import tachiyomi.domain.anime.model.Anime
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ReaderTransitionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    // KMK -->
    @ColorInt private val seedColor: Int? = null,
    // KMK <--
) :
    AbstractComposeView(context, attrs) {

    private var data: Data? by mutableStateOf(null)

    init {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    fun bind(transition: EpisodeTransition, downloadManager: DownloadManager, anime: Anime?) {
        data = if (anime != null) {
            Data(
                transition = transition,
                currEpisodeDownloaded = transition.from.pageLoader?.isLocal == true,
                goingToEpisodeDownloaded = anime.isLocal() ||
                    transition.to?.episode?.let { goingToEpisode ->
                        downloadManager.isEpisodeDownloaded(
                            episodeName = goingToEpisode.name,
                            episodeScanlator = goingToEpisode.scanlator,
                            animeTitle = /* SY --> */ anime.ogTitle, /* SY <-- */
                            sourceId = anime.source,
                            skipCache = true,
                        )
                    } ?: false,
            )
        } else {
            null
        }
    }

    @Composable
    override fun Content() {
        data?.let {
            // KMK -->
            val uiPreferences = Injekt.get<UiPreferences>()
            val themeCoverBased = uiPreferences.themeCoverBased().get()
            // KMK <--
            TachiyomiTheme(
                // KMK -->
                seedColor = seedColor?.let { Color(seedColor) }.takeIf { themeCoverBased },
                // KMK <--
            ) {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodySmall,
                    LocalContentColor provides MaterialTheme.colorScheme.onBackground,
                ) {
                    EpisodeTransition(
                        transition = it.transition,
                        currEpisodeDownloaded = it.currEpisodeDownloaded,
                        goingToEpisodeDownloaded = it.goingToEpisodeDownloaded,
                    )
                }
            }
        }
    }

    private data class Data(
        val transition: EpisodeTransition,
        val currEpisodeDownloaded: Boolean,
        val goingToEpisodeDownloaded: Boolean,
    )
}
