package mihon.feature.upcoming

import tachiyomi.domain.anime.model.Manga
import java.time.LocalDate

sealed interface UpcomingUIModel {
    data class Header(val date: LocalDate, val mangaCount: Int) : UpcomingUIModel
    data class Item(val manga: Manga) : UpcomingUIModel
}
