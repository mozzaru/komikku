package tachiyomi.domain.source.model

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SEpisode
import eu.kanade.tachiyomi.source.model.SAnime

class StubSource(
    override val id: Long,
    override val lang: String,
    override val name: String,
) : Source {

    private val isInvalid: Boolean = name.isBlank() || lang.isBlank()

    override suspend fun getMangaDetails(manga: SAnime): SAnime =
        throw SourceNotInstalledException()

    override suspend fun getChapterList(manga: SAnime): List<SEpisode> =
        throw SourceNotInstalledException()
    override suspend fun getPageList(chapter: SEpisode): List<Page> =
        throw SourceNotInstalledException()

    // KMK -->
    override suspend fun getRelatedMangaList(
        manga: SAnime,
        exceptionHandler: (Throwable) -> Unit,
        pushResults: suspend (relatedManga: Pair<String, List<SAnime>>, completed: Boolean) -> Unit,
    ) = throw SourceNotInstalledException()
    // KMK <--

    override fun toString(): String =
        if (!isInvalid) "$name (${lang.uppercase()})" else id.toString()

    companion object {
        fun from(source: Source): StubSource {
            return StubSource(id = source.id, lang = source.lang, name = source.name)
        }
    }
}

class SourceNotInstalledException : Exception()
