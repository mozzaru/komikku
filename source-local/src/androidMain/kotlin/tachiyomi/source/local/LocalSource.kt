package tachiyomi.source.local

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.UnmeteredSource
import eu.kanade.tachiyomi.source.model.AnimesPage
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SAnime
import eu.kanade.tachiyomi.source.model.SEpisode
import eu.kanade.tachiyomi.source.model.Video
import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import logcat.LogPriority
import mihon.core.archive.ZipWriter
import mihon.core.archive.archiveReader
import mihon.core.archive.epubReader
import nl.adaptivity.xmlutil.core.AndroidXmlReader
import nl.adaptivity.xmlutil.serialization.XML
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.storage.extension
import tachiyomi.core.common.storage.nameWithoutExtension
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.logcat
import tachiyomi.core.metadata.comicinfo.COMIC_INFO_FILE
import tachiyomi.core.metadata.comicinfo.ComicInfo
import tachiyomi.core.metadata.comicinfo.ComicInfoPublishingStatus
import tachiyomi.core.metadata.comicinfo.copyFromComicInfo
import tachiyomi.core.metadata.comicinfo.getComicInfo
import tachiyomi.core.metadata.tachiyomi.AnimeDetails
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.episode.service.EpisodeRecognition
import tachiyomi.i18n.MR
import tachiyomi.source.local.filter.OrderBy
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.Archive
import tachiyomi.source.local.io.Format
import tachiyomi.source.local.io.LocalSourceFileSystem
import tachiyomi.source.local.metadata.fillMetadata
import uy.kohesive.injekt.injectLazy
import java.io.InputStream
import java.nio.charset.StandardCharsets
import kotlin.time.Duration.Companion.days
import tachiyomi.domain.source.model.Source as DomainSource

actual class LocalSource(
    private val context: Context,
    private val fileSystem: LocalSourceFileSystem,
    private val coverManager: LocalCoverManager,
    // SY -->
    private val allowHiddenFiles: () -> Boolean,
    // SY <--
) : CatalogueSource, UnmeteredSource {

    private val json: Json by injectLazy()
    private val xml: XML by injectLazy()

    @Suppress("PrivatePropertyName")
    private val PopularFilters = FilterList(OrderBy.Popular(context))

    @Suppress("PrivatePropertyName")
    private val LatestFilters = FilterList(OrderBy.Latest(context))

    override val name: String = context.stringResource(MR.strings.local_source)

    override val id: Long = ID

    override val lang: String = "other"

    override fun toString() = name

    override val supportsLatest: Boolean = true

    // Browse related
    override suspend fun getPopularAnime(page: Int) = getSearchAnime(page, "", PopularFilters)

    override suspend fun getLatestUpdates(page: Int) = getSearchAnime(page, "", LatestFilters)

    override suspend fun getSearchAnime(page: Int, query: String, filters: FilterList): AnimesPage = withIOContext {
        val lastModifiedLimit = if (filters === LatestFilters) {
            System.currentTimeMillis() - LATEST_THRESHOLD
        } else {
            0L
        }
        // SY -->
        val allowLocalSourceHiddenFolders = allowHiddenFiles()
        // SY <--

        var animeDirs = fileSystem.getFilesInBaseDirectory()
            // Filter out files that are hidden and is not a folder
            .filter {
                it.isDirectory &&
                    /* SY --> */ (
                        !it.name.orEmpty().startsWith('.') ||
                            allowLocalSourceHiddenFolders
                        ) /* SY <-- */
            }
            .distinctBy { it.name }
            .filter {
                if (lastModifiedLimit == 0L && query.isBlank()) {
                    true
                } else if (lastModifiedLimit == 0L) {
                    it.name.orEmpty().contains(query, ignoreCase = true)
                } else {
                    it.lastModified() >= lastModifiedLimit
                }
            }

        filters.forEach { filter ->
            when (filter) {
                is OrderBy.Popular -> {
                    animeDirs = if (filter.state!!.ascending) {
                        animeDirs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name.orEmpty() })
                    } else {
                        animeDirs.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name.orEmpty() })
                    }
                }
                is OrderBy.Latest -> {
                    animeDirs = if (filter.state!!.ascending) {
                        animeDirs.sortedBy(UniFile::lastModified)
                    } else {
                        animeDirs.sortedByDescending(UniFile::lastModified)
                    }
                }
                else -> {
                    /* Do nothing */
                }
            }
        }

        val animes = animeDirs
            .map { animeDir ->
                async {
                    SAnime.create().apply {
                        title = animeDir.name.orEmpty()
                        url = animeDir.name.orEmpty()

                        // Try to find the cover
                        coverManager.find(animeDir.name.orEmpty())?.let {
                            thumbnail_url = it.uri.toString()
                        }
                    }
                }
            }
            .awaitAll()

        AnimesPage(animes, false)
    }

    // SY -->
    fun updateAnimeInfo(anime: SAnime) {
        val animeDirFiles = fileSystem.getFilesInAnimeDirectory(anime.url)
        val existingFile = animeDirFiles
            .firstOrNull { it.name == COMIC_INFO_FILE }
        val comicInfoArchiveFile = animeDirFiles.firstOrNull { it.name == COMIC_INFO_ARCHIVE }
        val comicInfoArchiveReader = comicInfoArchiveFile?.archiveReader(context)
        val existingComicInfo =
            (existingFile?.openInputStream() ?: comicInfoArchiveReader?.getInputStream(COMIC_INFO_FILE))?.use {
                AndroidXmlReader(it, StandardCharsets.UTF_8.name()).use { xmlReader ->
                    xml.decodeFromReader<ComicInfo>(xmlReader)
                }
            }
        val newComicInfo = if (existingComicInfo != null) {
            anime.run {
                existingComicInfo.copy(
                    series = ComicInfo.Series(title),
                    summary = description?.let { ComicInfo.Summary(it) },
                    writer = author?.let { ComicInfo.Writer(it) },
                    penciller = artist?.let { ComicInfo.Penciller(it) },
                    genre = genre?.let { ComicInfo.Genre(it) },
                    publishingStatus = ComicInfo.PublishingStatusTachiyomi(
                        ComicInfoPublishingStatus.toComicInfoValue(status.toLong()),
                    ),
                )
            }
        } else {
            anime.getComicInfo()
        }

        fileSystem.getAnimeDirectory(anime.url)?.let {
            copyComicInfoFile(
                xml.encodeToString(ComicInfo.serializer(), newComicInfo).byteInputStream(),
                it,
                comicInfoArchiveReader?.encrypted ?: false,
            )
        }
    }
    // SY <--

    // Anime details related
    override suspend fun getAnimeDetails(anime: SAnime): SAnime = withIOContext {
        coverManager.find(anime.url)?.let {
            anime.thumbnail_url = it.uri.toString()
        }

        // Augment anime details based on metadata files
        try {
            val animeDir = fileSystem.getAnimeDirectory(anime.url) ?: error("${anime.url} is not a valid directory")
            val animeDirFiles = animeDir.listFiles().orEmpty()

            val comicInfoFile = animeDirFiles
                .firstOrNull { it.name == COMIC_INFO_FILE }
            val noXmlFile = animeDirFiles
                .firstOrNull { it.name == ".noxml" }
            val legacyJsonDetailsFile = animeDirFiles
                .firstOrNull { it.extension == "json" }
            // SY -->
            val comicInfoArchiveFile = animeDirFiles
                .firstOrNull { it.name == COMIC_INFO_ARCHIVE }
            // SY <--

            when {
                // Top level ComicInfo.xml
                comicInfoFile != null -> {
                    noXmlFile?.delete()
                    setAnimeDetailsFromComicInfoFile(comicInfoFile.openInputStream(), anime)
                }
                // SY -->
                comicInfoArchiveFile != null -> {
                    noXmlFile?.delete()

                    comicInfoArchiveFile.archiveReader(context).getInputStream(COMIC_INFO_FILE)
                        ?.let { setAnimeDetailsFromComicInfoFile(it, anime) }
                }

                // SY <--

                // Old custom JSON format
                // TODO: remove support for this entirely after a while
                legacyJsonDetailsFile != null -> {
                    json.decodeFromStream<AnimeDetails>(legacyJsonDetailsFile.openInputStream()).run {
                        title?.let { anime.title = it }
                        author?.let { anime.author = it }
                        artist?.let { anime.artist = it }
                        description?.let { anime.description = it }
                        genre?.let { anime.genre = it.joinToString() }
                        status?.let { anime.status = it }
                    }
                    // Replace with ComicInfo.xml file
                    val comicInfo = anime.getComicInfo()
                    animeDir
                        .createFile(COMIC_INFO_FILE)
                        ?.openOutputStream()
                        ?.use {
                            val comicInfoString = xml.encodeToString(ComicInfo.serializer(), comicInfo)
                            it.write(comicInfoString.toByteArray())
                            legacyJsonDetailsFile.delete()
                        }
                }

                // Copy ComicInfo.xml from episode archive to top level if found
                noXmlFile == null -> {
                    val episodeArchives = animeDirFiles.filter(Archive::isSupported)

                    val copiedFile = copyComicInfoFileFromArchive(episodeArchives, animeDir)

                    // SY -->
                    if (copiedFile != null && copiedFile.name != COMIC_INFO_ARCHIVE) {
                        setAnimeDetailsFromComicInfoFile(copiedFile.openInputStream(), anime)
                    } else if (copiedFile != null && copiedFile.name == COMIC_INFO_ARCHIVE) {
                        copiedFile.archiveReader(context).getInputStream(COMIC_INFO_FILE)
                            ?.let { setAnimeDetailsFromComicInfoFile(it, anime) }
                    } // SY <--
                    else {
                        // Avoid re-scanning
                        animeDir.createFile(".noxml")
                    }
                }
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Error setting anime details from local metadata for ${anime.title}" }
        }

        return@withIOContext anime
    }

    private fun copyComicInfoFileFromArchive(episodeArchives: List<UniFile>, folder: UniFile): UniFile? {
        for (episode in episodeArchives) {
            episode.archiveReader(context).use { reader ->
                reader.getInputStream(COMIC_INFO_FILE)?.use { stream ->
                    return copyComicInfoFile(stream, folder, /* SY --> */ reader.encrypted /* SY <-- */)
                }
            }
        }
        return null
    }

    private fun copyComicInfoFile(
        comicInfoFileStream: InputStream,
        folder: UniFile,
        // SY -->
        encrypt: Boolean,
        // SY <--
    ): UniFile? {
        // SY -->
        if (encrypt) {
            val comicInfoArchiveFile = folder.createFile(COMIC_INFO_ARCHIVE)
            comicInfoArchiveFile?.let { archive ->
                ZipWriter(context, archive, encrypt = true).use { writer ->
                    writer.write(comicInfoFileStream.use { it.readBytes() }, COMIC_INFO_FILE)
                }
            }
            return comicInfoArchiveFile
        } else {
            // SY <--
            return folder.createFile(COMIC_INFO_FILE)?.apply {
                openOutputStream().use { outputStream ->
                    comicInfoFileStream.use { it.copyTo(outputStream) }
                }
            }
        }
    }

    private fun setAnimeDetailsFromComicInfoFile(stream: InputStream, anime: SAnime) {
        val comicInfo = AndroidXmlReader(stream, StandardCharsets.UTF_8.name()).use {
            xml.decodeFromReader<ComicInfo>(it)
        }

        anime.copyFromComicInfo(comicInfo)
    }

    // Episodes
    override suspend fun getEpisodeList(anime: SAnime): List<SEpisode> = withIOContext {
        val episodes = fileSystem.getFilesInAnimeDirectory(anime.url)
            // Only keep supported formats
            .filter { it.isDirectory || Archive.isSupported(it) || it.extension.equals("epub", true) }
            .map { episodeFile ->
                SEpisode.create().apply {
                    url = "${anime.url}/${episodeFile.name}"
                    name = if (episodeFile.isDirectory) {
                        episodeFile.name
                    } else {
                        episodeFile.nameWithoutExtension
                    }.orEmpty()
                    date_upload = episodeFile.lastModified()
                    episode_number = EpisodeRecognition
                        .parseEpisodeNumber(anime.title, this.name, this.episode_number.toDouble())
                        .toFloat()

                    val format = Format.valueOf(episodeFile)
                    if (format is Format.Epub) {
                        format.file.epubReader(context).use { epub ->
                            epub.fillMetadata(anime, this)
                        }
                    }
                }
            }
            .sortedWith { c1, c2 ->
                val c = c2.episode_number.compareTo(c1.episode_number)
                if (c == 0) c2.name.compareToCaseInsensitiveNaturalOrder(c1.name) else c
            }

        // Copy the cover from the first episode found if not available
        if (anime.thumbnail_url.isNullOrBlank()) {
            episodes.lastOrNull()?.let { episode ->
                updateCover(episode, anime)
            }
        }

        episodes
    }

    // Filters
    override fun getFilterList() = FilterList(OrderBy.Popular(context))

    // Unused stuff
    override suspend fun getVideoList(episode: SEpisode): List<Video> = throw UnsupportedOperationException("Unused")

    fun getFormat(episode: SEpisode): Format {
        try {
            val (animeDirName, episodeName) = episode.url.split('/', limit = 2)
            return fileSystem.getBaseDirectory()
                ?.findFile(animeDirName)
                ?.findFile(episodeName)
                ?.let(Format.Companion::valueOf)
                ?: throw Exception(context.stringResource(MR.strings.chapter_not_found))
        } catch (e: Format.UnknownFormatException) {
            throw Exception(context.stringResource(MR.strings.local_invalid_format))
        } catch (e: Exception) {
            throw e
        }
    }

    private fun updateCover(episode: SEpisode, anime: SAnime): UniFile? {
        return try {
            when (val format = getFormat(episode)) {
                is Format.Directory -> {
                    val entry = format.file.listFiles()
                        ?.sortedWith { f1, f2 ->
                            f1.name.orEmpty().compareToCaseInsensitiveNaturalOrder(
                                f2.name.orEmpty(),
                            )
                        }
                        ?.find {
                            !it.isDirectory && ImageUtil.isImage(it.name) { it.openInputStream() }
                        }

                    entry?.let { coverManager.update(anime, it.openInputStream()) }
                }
                is Format.Archive -> {
                    format.file.archiveReader(context).use { reader ->
                        val entry = reader.useEntries { entries ->
                            entries
                                .sortedWith { f1, f2 -> f1.name.compareToCaseInsensitiveNaturalOrder(f2.name) }
                                .find { it.isFile && ImageUtil.isImage(it.name) { reader.getInputStream(it.name)!! } }
                        }

                        entry?.let { coverManager.update(anime, reader.getInputStream(it.name)!!, reader.encrypted) }
                    }
                }
                is Format.Epub -> {
                    format.file.epubReader(context).use { epub ->
                        val entry = epub.getImagesFromPages().firstOrNull()

                        entry?.let { coverManager.update(anime, epub.getInputStream(it)!!) }
                    }
                }
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Error updating cover for ${anime.title}" }
            null
        }
    }

    companion object {
        const val ID = 0L
        const val HELP_URL = "https://mihon.app/docs/guides/local-source/"

        // SY -->
        const val COMIC_INFO_ARCHIVE = "ComicInfo.cbm"
        // SY <--

        private val LATEST_THRESHOLD = 7.days.inWholeMilliseconds
    }
}

fun Anime.isLocal(): Boolean = source == LocalSource.ID

fun Source.isLocal(): Boolean = id == LocalSource.ID

fun DomainSource.isLocal(): Boolean = id == LocalSource.ID
