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
import eu.kanade.presentation.manga.components.AnimeEpisodeListItem
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.reader.chapter.ReaderChapterItem
import eu.kanade.tachiyomi.util.lang.toRelativeString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import tachiyomi.domain.chapter.model.Episode
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ChapterListDialog(
    onDismissRequest: () -> Unit,
    chapters: ImmutableList<ReaderChapterItem>,
    onClickChapter: (Episode) -> Unit,
    onBookmark: (Episode) -> Unit,
    dateRelativeTime: Boolean,
) {
    val context = LocalContext.current
    val state = rememberLazyListState(chapters.indexOfFirst { it.isCurrent }.coerceAtLeast(0))
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
                items = chapters,
                key = { "episode-list-${it.episode.id}" },
            ) { chapterItem ->
                val activeDownload = downloadQueueState.find { it.episode.id == chapterItem.episode.id }
                val progress = activeDownload?.let {
                    downloadManager.progressFlow()
                        .filter { it.episode.id == chapterItem.episode.id }
                        .map { it.progress }
                        .collectAsState(0).value
                } ?: 0
                val downloaded = if (chapterItem.manga.isLocal()) {
                    true
                } else {
                    downloadManager.isEpisodeDownloaded(
                        chapterItem.episode.name,
                        chapterItem.episode.scanlator,
                        chapterItem.manga.ogTitle,
                        chapterItem.manga.source,
                    )
                }
                val downloadState = when {
                    activeDownload != null -> activeDownload.status
                    downloaded -> Download.State.DOWNLOADED
                    else -> Download.State.NOT_DOWNLOADED
                }
                AnimeEpisodeListItem(
                    title = chapterItem.episode.name,
                    date = chapterItem.episode.dateUpload
                        .takeIf { it > 0L }
                        ?.let {
                            LocalDate.ofInstant(
                                Instant.ofEpochMilli(it),
                                ZoneId.systemDefault(),
                            ).toRelativeString(context, dateRelativeTime, chapterItem.dateFormat)
                        },
                    watchProgress = null,
                    scanlator = chapterItem.episode.scanlator,
                    sourceName = null,
                    seen = chapterItem.episode.seen,
                    bookmark = chapterItem.episode.bookmark,
                    selected = false,
                    downloadIndicatorEnabled = false,
                    downloadStateProvider = { downloadState },
                    downloadProgressProvider = { progress },
                    episodeSwipeStartAction = LibraryPreferences.EpisodeSwipeAction.ToggleBookmark,
                    episodeSwipeEndAction = LibraryPreferences.EpisodeSwipeAction.ToggleBookmark,
                    onLongClick = { /*TODO*/ },
                    onClick = { onClickChapter(chapterItem.episode) },
                    onDownloadClick = null,
                    onEpisodeSwipe = {
                        onBookmark(chapterItem.episode)
                    },
                )
            }
        }
    }
}
