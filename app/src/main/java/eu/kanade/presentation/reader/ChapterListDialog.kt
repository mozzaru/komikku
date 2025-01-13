package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.anime.components.AnimeEpisodeListItem
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.reader.episode.ReaderEpisodeItem
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import eu.kanade.tachiyomi.util.lang.toRelativeString
import exh.metadata.MetadataUtil
import exh.source.isEhBasedManga
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun EpisodeListDialog(
    onDismissRequest: () -> Unit,
    screenModel: ReaderSettingsScreenModel,
    episodes: ImmutableList<ReaderEpisodeItem>,
    onClickEpisode: (Episode) -> Unit,
    onBookmark: (Episode) -> Unit,
    dateRelativeTime: Boolean,
) {
    val anime by screenModel.animeFlow.collectAsState()
    val context = LocalContext.current
    val state = rememberLazyListState(episodes.indexOfFirst { it.isCurrent }.coerceAtLeast(0))
    val downloadManager: DownloadManager = remember { Injekt.get() }
    val downloadQueueState by downloadManager.queueState.collectAsState()

    AdaptiveSheet(
        onDismissRequest = onDismissRequest,
    ) {
        LazyColumn(
            state = state,
            modifier = Modifier.heightIn(min = 200.dp, max = 500.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            items(
                items = episodes,
                key = { "episode-list-${it.episode.id}" },
            ) { episodeItem ->
                val activeDownload = downloadQueueState.find { it.episode.id == episodeItem.episode.id }
                val progress = activeDownload?.let {
                    downloadManager.progressFlow()
                        .filter { it.episode.id == episodeItem.episode.id }
                        .map { it.progress }
                        .collectAsState(0).value
                } ?: 0
                val downloaded = if (episodeItem.anime.isLocal()) {
                    true
                } else {
                    downloadManager.isEpisodeDownloaded(
                        episodeItem.episode.name,
                        episodeItem.episode.scanlator,
                        episodeItem.anime.ogTitle,
                        episodeItem.anime.source,
                    )
                }
                val downloadState = when {
                    activeDownload != null -> activeDownload.status
                    downloaded -> Download.State.DOWNLOADED
                    else -> Download.State.NOT_DOWNLOADED
                }
                AnimeEpisodeListItem(
                    title = episodeItem.episode.name,
                    date = episodeItem.episode.dateUpload
                        .takeIf { it > 0L }
                        ?.let {
                            // SY -->
                            if (anime?.isEhBasedManga() == true) {
                                MetadataUtil.EX_DATE_FORMAT
                                    .format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()))
                            } else {
                                LocalDate.ofInstant(
                                    Instant.ofEpochMilli(it),
                                    ZoneId.systemDefault(),
                                ).toRelativeString(context, dateRelativeTime, episodeItem.dateFormat)
                            }
                            // SY <--
                        },
                    readProgress = null,
                    scanlator = episodeItem.episode.scanlator,
                    sourceName = null,
                    read = episodeItem.episode.seen,
                    bookmark = episodeItem.episode.bookmark,
                    selected = false,
                    downloadIndicatorEnabled = false,
                    downloadStateProvider = { downloadState },
                    downloadProgressProvider = { progress },
                    episodeSwipeStartAction = LibraryPreferences.EpisodeSwipeAction.ToggleBookmark,
                    episodeSwipeEndAction = LibraryPreferences.EpisodeSwipeAction.ToggleBookmark,
                    onLongClick = { /*TODO*/ },
                    onClick = { onClickEpisode(episodeItem.episode) },
                    onDownloadClick = null,
                    onEpisodeSwipe = {
                        onBookmark(episodeItem.episode)
                    },
                )
            }
        }
    }
}
