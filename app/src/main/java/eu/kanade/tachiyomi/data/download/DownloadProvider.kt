package eu.kanade.tachiyomi.data.download

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.util.storage.DiskUtil
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.storage.displayablePath
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * This class is used to provide the directories where the downloads should be saved.
 * It uses the following path scheme: /<root downloads dir>/<source name>/<manga>/<chapter>
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
     * @param animeTitle the title of the manga to query.
     * @param source the source of the manga.
     */
    internal fun getAnimeDir(animeTitle: String, source: Source): UniFile {
        try {
            return downloadsDir!!
                .createDirectory(getSourceDirName(source))!!
                .createDirectory(getAnimeDirName(animeTitle))!!
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Invalid download directory" }
            throw Exception(
                context.stringResource(
                    MR.strings.invalid_location,
                    (downloadsDir?.displayablePath ?: "") +
                        "/${getSourceDirName(source)}/${getAnimeDirName(animeTitle)}",
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
     * @param animeTitle the title of the manga to query.
     * @param source the source of the manga.
     */
    fun findAnimeDir(animeTitle: String, source: Source): UniFile? {
        val sourceDir = findSourceDir(source)
        return sourceDir?.findFile(getAnimeDirName(animeTitle))
    }

    /**
     * Returns the download directory for a chapter if it exists.
     *
     * @param episodeName the name of the chapter to query.
     * @param episodeScanlator scanlator of the chapter to query
     * @param animeTitle the title of the manga to query.
     * @param source the source of the chapter.
     */
    fun findEpisodeDir(episodeName: String, episodeScanlator: String?, animeTitle: String, source: Source): UniFile? {
        val animeDir = findAnimeDir(animeTitle, source)
        return getValidEpisodeDirNames(episodeName, episodeScanlator).asSequence()
            .mapNotNull { animeDir?.findFile(it) }
            .firstOrNull()
    }

    /**
     * Returns a list of downloaded directories for the chapters that exist.
     *
     * @param chapters the chapters to query.
     * @param manga the manga of the chapter.
     * @param source the source of the chapter.
     */
    fun findEpisodeDirs(chapters: List<Chapter>, manga: Manga, source: Source): Pair<UniFile?, List<UniFile>> {
        val animeDir = findAnimeDir(/* SY --> */ manga.ogTitle /* SY <-- */, source) ?: return null to emptyList()
        return animeDir to chapters.mapNotNull { episode ->
            getValidEpisodeDirNames(episode.name, episode.scanlator).asSequence()
                .mapNotNull { animeDir.findFile(it) }
                .firstOrNull()
        }
    }

    // SY -->
    /**
     * Returns a list of all files in manga directory
     *
     * @param chapters the chapters to query.
     * @param manga the manga of the chapter.
     * @param source the source of the chapter.
     */
    fun findUnmatchedEpisodeDirs(
        chapters: List<Chapter>,
        manga: Manga,
        source: Source,
    ): List<UniFile> {
        val animeDir = findAnimeDir(/* SY --> */ manga.ogTitle /* SY <-- */, source) ?: return emptyList()
        return animeDir.listFiles().orEmpty().asList().filter {
            chapters.find { chp ->
                getValidEpisodeDirNames(chp.name, chp.scanlator).any { dir ->
                    animeDir.findFile(dir) != null
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
     * @param animeTitle the title of the manga to query.
     */
    fun getAnimeDirName(animeTitle: String): String {
        return DiskUtil.buildValidFilename(animeTitle)
    }

    /**
     * Returns the chapter directory name for a chapter.
     *
     * @param episodeName the name of the chapter to query.
     * @param episodeScanlator scanlator of the chapter to query
     */
    fun getEpisodeDirName(episodeName: String, episodeScanlator: String?): String {
        val newEpisodeName = sanitizeEpisodeName(episodeName)
        return DiskUtil.buildValidFilename(
            when {
                !episodeScanlator.isNullOrBlank() -> "${episodeScanlator}_$newEpisodeName"
                else -> newEpisodeName
            },
        )
    }

    /**
     * Return the new name for the chapter (in case it's empty or blank)
     *
     * @param episodeName the name of the chapter
     */
    private fun sanitizeEpisodeName(episodeName: String): String {
        return episodeName.ifBlank {
            "Episode"
        }
    }

    fun isEpisodeDirNameChanged(oldChapter: Chapter, newChapter: Chapter): Boolean {
        return oldChapter.name != newChapter.name ||
            oldChapter.scanlator?.takeIf { it.isNotBlank() } != newChapter.scanlator?.takeIf { it.isNotBlank() }
    }

    /**
     * Returns valid downloaded chapter directory names.
     *
     * @param episodeName the name of the chapter to query.
     * @param episodeScanlator scanlator of the chapter to query
     */
    fun getValidEpisodeDirNames(episodeName: String, episodeScanlator: String?): List<String> {
        val episodeDirName = getEpisodeDirName(episodeName, episodeScanlator)
        return buildList(2) {
            // Folder of images
            add(episodeDirName)

            // Archived chapters
            add("$episodeDirName.cbz")
        }
    }
}
