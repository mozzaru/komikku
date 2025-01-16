package eu.kanade.tachiyomi.data.download

import android.content.Context
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tachiyomi.domain.anime.model.Manga
import tachiyomi.domain.episode.model.Episode
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Class used to keep a list of episodes for future deletion.
 *
 * @param context the application context.
 */
class DownloadPendingDeleter(
    context: Context,
    private val json: Json = Injekt.get(),
) {

    /**
     * Preferences used to store the list of episodes to delete.
     */
    private val preferences = context.getSharedPreferences("chapters_to_delete", Context.MODE_PRIVATE)

    /**
     * Last added episode, used to avoid decoding from the preference too often.
     */
    private var lastAddedEntry: Entry? = null

    /**
     * Adds a list of episodes for future deletion.
     *
     * @param episodes the episodes to be deleted.
     * @param manga the manga of the episodes.
     */
    @Synchronized
    fun addChapters(episodes: List<Episode>, manga: Manga) {
        val lastEntry = lastAddedEntry

        val newEntry = if (lastEntry != null && lastEntry.manga.id == manga.id) {
            // Append new episodes
            val newChapters = lastEntry.chapters.addUniqueById(episodes)

            // If no episodes were added, do nothing
            if (newChapters.size == lastEntry.chapters.size) return

            // Last entry matches the manga, reuse it to avoid decoding json from preferences
            lastEntry.copy(chapters = newChapters)
        } else {
            val existingEntry = preferences.getString(manga.id.toString(), null)
            if (existingEntry != null) {
                // Existing entry found on preferences, decode json and add the new episode
                val savedEntry = json.decodeFromString<Entry>(existingEntry)

                // Append new episodes
                val newChapters = savedEntry.chapters.addUniqueById(episodes)

                // If no episodes were added, do nothing
                if (newChapters.size == savedEntry.chapters.size) return

                savedEntry.copy(chapters = newChapters)
            } else {
                // No entry has been found yet, create a new one
                Entry(episodes.map { it.toEntry() }, manga.toEntry())
            }
        }

        // Save current state
        val json = json.encodeToString(newEntry)
        preferences.edit {
            putString(newEntry.manga.id.toString(), json)
        }
        lastAddedEntry = newEntry
    }

    /**
     * Returns the list of episodes to be deleted grouped by its manga.
     *
     * Note: the returned list of manga and episodes only contain basic information needed by the
     * downloader, so don't use them for anything else.
     */
    @Synchronized
    fun getPendingChapters(): Map<Manga, List<Episode>> {
        val entries = decodeAll()
        preferences.edit {
            clear()
        }
        lastAddedEntry = null

        return entries.associate { (chapters, manga) ->
            manga.toModel() to chapters.map { it.toModel() }
        }
    }

    /**
     * Decodes all the episodes from preferences.
     */
    private fun decodeAll(): List<Entry> {
        return preferences.all.values.mapNotNull { rawEntry ->
            try {
                (rawEntry as? String)?.let { json.decodeFromString<Entry>(it) }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Returns a copy of episode entries ensuring no duplicates by episode id.
     */
    private fun List<ChapterEntry>.addUniqueById(episodes: List<Episode>): List<ChapterEntry> {
        val newList = toMutableList()
        for (chapter in episodes) {
            if (none { it.id == chapter.id }) {
                newList.add(chapter.toEntry())
            }
        }
        return newList
    }

    /**
     * Returns a manga entry from a manga model.
     */
    private fun Manga.toEntry() = MangaEntry(id, url, /* SY --> */ ogTitle /* SY <-- */, source)

    /**
     * Returns a episode entry from a episode model.
     */
    private fun Episode.toEntry() = ChapterEntry(id, url, name, scanlator)

    /**
     * Returns a manga model from a manga entry.
     */
    private fun MangaEntry.toModel() = Manga.create().copy(
        url = url,
        // SY -->
        ogTitle = title,
        // SY <--
        source = source,
        id = id,
    )

    /**
     * Returns a episode model from a episode entry.
     */
    private fun ChapterEntry.toModel() = Episode.create().copy(
        id = id,
        url = url,
        name = name,
        scanlator = scanlator,
    )

    /**
     * Class used to save an entry of episodes with their manga into preferences.
     */
    @Serializable
    private data class Entry(
        val chapters: List<ChapterEntry>,
        val manga: MangaEntry,
    )

    /**
     * Class used to save an entry for a episode into preferences.
     */
    @Serializable
    private data class ChapterEntry(
        val id: Long,
        val url: String,
        val name: String,
        val scanlator: String? = null,
    )

    /**
     * Class used to save an entry for a manga into preferences.
     */
    @Serializable
    private data class MangaEntry(
        val id: Long,
        val url: String,
        val title: String,
        val source: Long,
    )
}
