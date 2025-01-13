package exh.util

import android.content.Context
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale

fun Anime.animeType(context: Context): String {
    return context.stringResource(
        when (animeType()) {
            AnimeType.TYPE_WEBTOON -> SYMR.strings.entry_type_webtoon
            AnimeType.TYPE_MANHWA -> SYMR.strings.entry_type_manhwa
            AnimeType.TYPE_MANHUA -> SYMR.strings.entry_type_manhua
            AnimeType.TYPE_COMIC -> SYMR.strings.entry_type_comic
            else -> SYMR.strings.entry_type_anime
        },
    ).lowercase(Locale.getDefault())
}

/**
 * The type of comic the anime is (ie. anime, manhwa, manhua)
 */
fun Anime.animeType(sourceName: String? = Injekt.get<SourceManager>().get(source)?.name): AnimeType {
    val currentTags = genre.orEmpty()
    return when {
        currentTags.any { tag -> isAnimeTag(tag) } -> {
            AnimeType.TYPE_MANGA
        }
        currentTags.any { tag -> isWebtoonTag(tag) } || sourceName?.let { isWebtoonSource(it) } == true -> {
            AnimeType.TYPE_WEBTOON
        }
        currentTags.any { tag -> isComicTag(tag) } || sourceName?.let { isComicSource(it) } == true -> {
            AnimeType.TYPE_COMIC
        }
        currentTags.any { tag -> isManhuaTag(tag) } || sourceName?.let { isManhuaSource(it) } == true -> {
            AnimeType.TYPE_MANHUA
        }
        currentTags.any { tag -> isManhwaTag(tag) } || sourceName?.let { isManhwaSource(it) } == true -> {
            AnimeType.TYPE_MANHWA
        }
        else -> {
            AnimeType.TYPE_MANGA
        }
    }
}

/**
 * The type the reader should use. Different from anime type as certain anime has different
 * read types
 */
fun Anime.defaultReaderType(type: AnimeType = animeType()): Int? {
    return if (type == AnimeType.TYPE_MANHWA || type == AnimeType.TYPE_WEBTOON) {
        ReadingMode.WEBTOON.flagValue
    } else {
        null
    }
}

private fun isAnimeTag(tag: String): Boolean {
    return tag.contains("anime", true) ||
        tag.contains("манга", true)
}

private fun isManhuaTag(tag: String): Boolean {
    return tag.contains("manhua", true) ||
        tag.contains("маньхуа", true)
}

private fun isManhwaTag(tag: String): Boolean {
    return tag.contains("manhwa", true) ||
        tag.contains("манхва", true)
}

private fun isComicTag(tag: String): Boolean {
    return tag.contains("comic", true) ||
        tag.contains("комикс", true)
}

private fun isWebtoonTag(tag: String): Boolean {
    return tag.contains("long strip", true) ||
        tag.contains("webtoon", true)
}

/*private fun isAnimeSource(sourceName: String): Boolean {
    return
}*/

private fun isManhwaSource(sourceName: String): Boolean {
    return sourceName.contains("hiperdex", true) ||
        sourceName.contains("hmanhwa", true) ||
        sourceName.contains("instamanhwa", true) ||
        sourceName.contains("manhwa18", true) ||
        sourceName.contains("manhwa68", true) ||
        sourceName.contains("manhwa365", true) ||
        sourceName.contains("manhwahentaime", true) ||
        sourceName.contains("manhwaanime", true) ||
        sourceName.contains("manhwatop", true) ||
        sourceName.contains("manhwa club", true) ||
        sourceName.contains("manytoon", true) ||
        sourceName.contains("manwha", true) ||
        sourceName.contains("readmanhwa", true) ||
        sourceName.contains("skyanime", true) ||
        sourceName.contains("toonily", true) ||
        sourceName.contains("webtoonxyz", true)
}

private fun isWebtoonSource(sourceName: String): Boolean {
    return sourceName.contains("animetoon", true) ||
        sourceName.contains("mananime", true) ||
        // sourceName.contains("tapas", true) ||
        sourceName.contains("toomics", true) ||
        sourceName.contains("webcomics", true) ||
        sourceName.contains("webtoons", true) ||
        sourceName.contains("webtoon", true)
}

private fun isComicSource(sourceName: String): Boolean {
    return sourceName.contains("8muses", true) ||
        sourceName.contains("allporncomic", true) ||
        sourceName.contains("ciayo comics", true) ||
        sourceName.contains("comicextra", true) ||
        sourceName.contains("comicpunch", true) ||
        sourceName.contains("cyanide", true) ||
        sourceName.contains("dilbert", true) ||
        sourceName.contains("eggporncomics", true) ||
        sourceName.contains("existential comics", true) ||
        sourceName.contains("hiveworks comics", true) ||
        sourceName.contains("milftoon", true) ||
        sourceName.contains("myhentaicomics", true) ||
        sourceName.contains("myhentaigallery", true) ||
        sourceName.contains("gunnerkrigg", true) ||
        sourceName.contains("oglaf", true) ||
        sourceName.contains("patch friday", true) ||
        sourceName.contains("porncomix", true) ||
        sourceName.contains("questionable content", true) ||
        sourceName.contains("readcomiconline", true) ||
        sourceName.contains("read comics online", true) ||
        sourceName.contains("swords comic", true) ||
        sourceName.contains("teabeer comics", true) ||
        sourceName.contains("xkcd", true)
}

private fun isManhuaSource(sourceName: String): Boolean {
    return sourceName.contains("1st kiss manhua", true) ||
        sourceName.contains("hero manhua", true) ||
        sourceName.contains("manhuabox", true) ||
        sourceName.contains("manhuaus", true) ||
        sourceName.contains("manhuas world", true) ||
        sourceName.contains("manhuas.net", true) ||
        sourceName.contains("readmanhua", true) ||
        sourceName.contains("wuxiaworld", true) ||
        sourceName.contains("manhua", true)
}

enum class AnimeType {
    TYPE_MANGA,
    TYPE_MANHWA,
    TYPE_MANHUA,
    TYPE_COMIC,
    TYPE_WEBTOON,
}
