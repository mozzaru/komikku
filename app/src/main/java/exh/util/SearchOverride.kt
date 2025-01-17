package exh.util

import android.content.Context
import eu.kanade.domain.anime.model.toSManga
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.online.UrlImportableSource
import exh.GalleryAddEvent
import exh.GalleryAdder
import rx.Observable
import tachiyomi.core.common.util.lang.runAsObservable

private val galleryAdder by lazy {
    GalleryAdder()
}

/**
 * A version of fetchSearchAnime that supports URL importing
 */
fun UrlImportableSource.urlImportFetchSearchManga(
    context: Context,
    query: String,
    fail: () -> Observable<AnimesPage>,
): Observable<AnimesPage> =
    when {
        query.startsWith("http://") || query.startsWith("https://") -> {
            runAsObservable {
                galleryAdder.addGallery(context, query, false, this@urlImportFetchSearchManga)
            }
                .map { res ->
                    AnimesPage(
                        if (res is GalleryAddEvent.Success) {
                            listOf(res.manga.toSManga())
                        } else {
                            emptyList()
                        },
                        false,
                    )
                }
        }
        else -> fail()
    }

/**
 * A version of fetchSearchAnime that supports URL importing
 */
suspend fun UrlImportableSource.urlImportFetchSearchMangaSuspend(
    context: Context,
    query: String,
    fail: suspend () -> AnimesPage,
): AnimesPage =
    when {
        query.startsWith("http://") || query.startsWith("https://") -> {
            val res = galleryAdder.addGallery(
                context = context,
                url = query,
                fav = false,
                forceSource = this,
            )

            AnimesPage(
                if (res is GalleryAddEvent.Success) {
                    listOf(res.manga.toSManga())
                } else {
                    emptyList()
                },
                false,
            )
        }
        else -> fail()
    }
