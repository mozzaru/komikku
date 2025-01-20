package eu.kanade.tachiyomi.data.download

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.util.storage.DiskUtil
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.storage.displayablePath
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * This class is used to provide the directories where the downloads should be saved.
 * It uses the following path scheme: /<root downloads dir>/<source name>/<manga>/<episode>
 *
 * @param context the application context.
 */
class DownloadProvider(
    private val context: Context,
    private val storageManager: StorageManager = Injekt.get(),
) {

    private val downloadsDir: UniFile?
        get() = storageManager.getDownloadsDirectory()

    /**
     * Returns the download directory for a manga. For internal use only.
     *
     * @param mangaTitle the title of the manga to query.
     * @param source the source of the manga.
     */
    internal fun getMangaDir(mangaTitle: String, source: Source): UniFile {
        try {
            return downloadsDir!!
                .createDirectory(getSourceDirName(source))!!
                .createDirectory(getMangaDirName(mangaTitle))!!
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Invalid download directory" }
            throw Exception(
                context.stringResource(
                    MR.strings.invalid_location,
                    (downloadsDir?.displayablePath ?: "") +
                        "/${getSourceDirName(source)}/${getMangaDirName(mangaTitle)}",
                ),
            )
        }
    }

    /**
     * Returns the download directory for a source if it exists.
     *
     * @param source the source to query.
     */
    fun findSourceDir(source: Source): UniFile? {
        return downloadsDir?.findFile(getSourceDirName(source))
    }

    /**
     * Returns the download directory for a manga if it exists.
     *
     * @param mangaTitle the title of the manga to query.
     * @param source the source of the manga.
     */
    fun findMangaDir(mangaTitle: String, source: Source): UniFile? {
        val sourceDir = findSourceDir(source)
        return sourceDir?.findFile(getMangaDirName(mangaTitle))
    }

    /**
     * Returns the download directory for a episode if it exists.
     *
     * @param chapterName the name of the episode to query.
     * @param chapterScanlator scanlator of the episode to query
     * @param mangaTitle the title of the manga to query.
     * @param source the source of the episode.
     */
    fun findChapterDir(chapterName: String, chapterScanlator: String?, mangaTitle: String, source: Source): UniFile? {
        val mangaDir = findMangaDir(mangaTitle, source)
        return getValidChapterDirNames(chapterName, chapterScanlator).asSequence()
            .mapNotNull { mangaDir?.findFile(it) }
            .firstOrNull()
    }

    /**
     * Returns a list of downloaded directories for the episodes that exist.
     *
     * @param episodes the episodes to query.
     * @param manga the manga of the episode.
     * @param source the source of the episode.
     */
    fun findChapterDirs(episodes: List<Episode>, manga: Anime, source: Source): Pair<UniFile?, List<UniFile>> {
        val mangaDir = findMangaDir(/* SY --> */ manga.ogTitle /* SY <-- */, source) ?: return null to emptyList()
        return mangaDir to episodes.mapNotNull { chapter ->
            getValidChapterDirNames(chapter.name, chapter.scanlator).asSequence()
                .mapNotNull { mangaDir.findFile(it) }
                .firstOrNull()
        }
    }

    // SY -->
    /**
     * Returns a list of all files in manga directory
     *
     * @param episodes the episodes to query.
     * @param manga the manga of the episode.
     * @param source the source of the episode.
     */
    fun findUnmatchedChapterDirs(
        episodes: List<Episode>,
        manga: Anime,
        source: Source,
    ): List<UniFile> {
        val mangaDir = findMangaDir(/* SY --> */ manga.ogTitle /* SY <-- */, source) ?: return emptyList()
        return mangaDir.listFiles().orEmpty().asList().filter {
            episodes.find { chp ->
                getValidChapterDirNames(chp.name, chp.scanlator).any { dir ->
                    mangaDir.findFile(dir) != null
                }
            } == null ||
                it.name?.endsWith(Downloader.TMP_DIR_SUFFIX) == true
        }
    }
    // SY <--

    /**
     * Returns the download directory name for a source.
     *
     * @param source the source to query.
     */
    fun getSourceDirName(source: Source): String {
        return DiskUtil.buildValidFilename(source.toString())
    }

    /**
     * Returns the download directory name for a manga.
     *
     * @param mangaTitle the title of the manga to query.
     */
    fun getMangaDirName(mangaTitle: String): String {
        return DiskUtil.buildValidFilename(mangaTitle)
    }

    /**
     * Returns the episode directory name for a episode.
     *
     * @param chapterName the name of the episode to query.
     * @param chapterScanlator scanlator of the episode to query
     */
    fun getChapterDirName(chapterName: String, chapterScanlator: String?): String {
        val newChapterName = sanitizeChapterName(chapterName)
        return DiskUtil.buildValidFilename(
            when {
                !chapterScanlator.isNullOrBlank() -> "${chapterScanlator}_$newChapterName"
                else -> newChapterName
            },
        )
    }

    /**
     * Return the new name for the episode (in case it's empty or blank)
     *
     * @param chapterName the name of the episode
     */
    private fun sanitizeChapterName(chapterName: String): String {
        return chapterName.ifBlank {
            "Episode"
        }
    }

    fun isChapterDirNameChanged(oldEpisode: Episode, newEpisode: Episode): Boolean {
        return oldEpisode.name != newEpisode.name ||
            oldEpisode.scanlator?.takeIf { it.isNotBlank() } != newEpisode.scanlator?.takeIf { it.isNotBlank() }
    }

    /**
     * Returns valid downloaded episode directory names.
     *
     * @param chapterName the name of the episode to query.
     * @param chapterScanlator scanlator of the episode to query
     */
    fun getValidChapterDirNames(chapterName: String, chapterScanlator: String?): List<String> {
        val chapterDirName = getChapterDirName(chapterName, chapterScanlator)
        return buildList(2) {
            // Folder of images
            add(chapterDirName)

            // Archived episodes
            add("$chapterDirName.cbz")
        }
    }
}
