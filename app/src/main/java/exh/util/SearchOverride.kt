package exh.util

import android.content.Context
import eu.kanade.domain.anime.model.toSAnime
import eu.kanade.tachiyomi.source.model.AnimesPage
import eu.kanade.tachiyomi.source.online.UrlImportableSource
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
fun UrlImportableSource.urlImportFetchSearchAnime(
    context: Context,
    query: String,
    fail: () -> Observable<AnimesPage>,
): Observable<AnimesPage> =
    when {
        query.startsWith("http://") || query.startsWith("https://") -> {
            runAsObservable {
                galleryAdder.addGallery(context, query, false, this@urlImportFetchSearchAnime)
            }
                .map { res ->
                    AnimesPage(
                        if (res is GalleryAddEvent.Success) {
                            listOf(res.anime.toSAnime())
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
suspend fun UrlImportableSource.urlImportFetchSearchAnimeSuspend(
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
                    listOf(res.anime.toSAnime())
                } else {
                    emptyList()
                },
                false,
            )
        }
        else -> fail()
    }
