package mihon.domain.upcoming.interactor

import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.anime.interactor.GetLibraryAnime
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.repository.AnimeRepository
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_HAS_UNREAD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.ANIME_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_READ
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_OUTSIDE_RELEASE_PERIOD
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.LocalDate
import java.time.ZoneId

class GetUpcomingAnime(
    private val animeRepository: AnimeRepository,
) {
    // KMK -->
    private val libraryPreferences: LibraryPreferences = Injekt.get()
    private val getLibraryAnime: GetLibraryAnime = Injekt.get()
    // KMK <--

    private val includedStatuses = setOf(
        SAnime.ONGOING.toLong(),
        SAnime.PUBLISHING_FINISHED.toLong(),
    )

    suspend fun subscribe(): Flow<List<Anime>> {
        return animeRepository.getUpcomingManga(includedStatuses)
    }

    // KMK -->
    suspend fun updatingMangas(): List<Anime> {
        val libraryManga = getLibraryAnime.await()

        val categoriesToUpdate = libraryPreferences.updateCategories().get().map(String::toLong)
        val includedManga = if (categoriesToUpdate.isNotEmpty()) {
            libraryManga.filter { it.category in categoriesToUpdate }
        } else {
            libraryManga
        }

        val categoriesToExclude = libraryPreferences.updateCategoriesExclude().get().map { it.toLong() }
        val excludedMangaIds = if (categoriesToExclude.isNotEmpty()) {
            libraryManga.filter { it.category in categoriesToExclude }.map { it.manga.id }
        } else {
            emptyList()
        }

        val listToUpdate = includedManga
            .filterNot { it.manga.id in excludedMangaIds }

        val restrictions = libraryPreferences.autoUpdateAnimeRestrictions().get()
        val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000

        return listToUpdate
            .distinctBy { it.manga.id }
            .filter {
                when {
                    it.manga.updateStrategy != UpdateStrategy.ALWAYS_UPDATE -> false

                    ANIME_NON_COMPLETED in restrictions && it.manga.status.toInt() == SAnime.COMPLETED -> false

                    MANGA_HAS_UNREAD in restrictions && it.unreadCount != 0L -> false

                    MANGA_NON_READ in restrictions && it.totalChapters > 0L && !it.hasStarted -> false

                    MANGA_OUTSIDE_RELEASE_PERIOD in restrictions && it.manga.nextUpdate < today -> false

                    else -> true
                }
            }
            .map { it.manga }
            .sortedBy { it.nextUpdate }
    }
    // KMK <--
}
