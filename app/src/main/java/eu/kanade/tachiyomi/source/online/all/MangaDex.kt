package eu.kanade.tachiyomi.source.online.all

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.model.AnimesPage
import eu.kanade.tachiyomi.source.model.MetadataAnimesPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SAnime
import eu.kanade.tachiyomi.source.model.SEpisode
import eu.kanade.tachiyomi.source.online.FollowsSource
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.LoginSource
import eu.kanade.tachiyomi.source.online.MetadataSource
import eu.kanade.tachiyomi.source.online.NamespaceSource
import eu.kanade.tachiyomi.source.online.RandomAnimeSource
import eu.kanade.tachiyomi.source.online.UrlImportableSource
import exh.md.dto.AnimeDto
import exh.md.dto.StatisticsAnimeDto
import exh.md.handlers.AnimeHandler
import exh.md.handlers.ApiAnimeParser
import exh.md.handlers.AzukiHandler
import exh.md.handlers.BilibiliHandler
import exh.md.handlers.ComikeyHandler
import exh.md.handlers.FollowsHandler
import exh.md.handlers.MangaHotHandler
import exh.md.handlers.MangaPlusHandler
import exh.md.handlers.NamicomiHandler
import exh.md.handlers.PageHandler
import exh.md.handlers.SimilarHandler
import exh.md.network.MangaDexLoginHelper
import exh.md.service.MangaDexAuthService
import exh.md.service.MangaDexService
import exh.md.service.SimilarService
import exh.md.utils.FollowStatus
import exh.md.utils.MdApi
import exh.md.utils.MdLang
import exh.md.utils.MdUtil
import exh.metadata.metadata.MangaDexSearchMetadata
import exh.source.DelegatedHttpSource
import okhttp3.OkHttpClient
import okhttp3.Response
import rx.Observable
import tachiyomi.core.common.util.lang.runAsObservable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.reflect.KClass

@Suppress("OverridingDeprecatedMember")
class MangaDex(delegate: HttpSource, val context: Context) :
    DelegatedHttpSource(delegate),
    MetadataSource<MangaDexSearchMetadata, Triple<AnimeDto, List<String>, StatisticsAnimeDto>>,
    UrlImportableSource,
    FollowsSource,
    LoginSource,
    RandomAnimeSource,
    NamespaceSource {
    override val lang: String = delegate.lang

    private val mdLang by lazy {
        MdLang.fromExt(lang) ?: MdLang.ENGLISH
    }

    override val matchingHosts: List<String> = listOf("mangadex.org", "www.mangadex.org")

    val trackPreferences: TrackPreferences by injectLazy()
    val mdList: MdList by lazy { Injekt.get<TrackerManager>().mdList }

    private val sourcePreferences: SharedPreferences by lazy {
        context.getSharedPreferences("source_$id", 0x0000)
    }

    private val loginHelper = MangaDexLoginHelper(network.client, trackPreferences, mdList, mdList.interceptor)

    override val baseHttpClient: OkHttpClient = delegate.client.newBuilder()
        .addInterceptor(mdList.interceptor)
        .build()

    private fun dataSaver() = sourcePreferences.getBoolean(getDataSaverPreferenceKey(mdLang.lang), false)
    private fun usePort443Only() = sourcePreferences.getBoolean(getStandardHttpsPreferenceKey(mdLang.lang), false)
    private fun blockedGroups() = sourcePreferences.getString(getBlockedGroupsPrefKey(mdLang.lang), "").orEmpty()
    private fun blockedUploaders() = sourcePreferences.getString(getBlockedUploaderPrefKey(mdLang.lang), "").orEmpty()
    private fun coverQuality() = sourcePreferences.getString(getCoverQualityPrefKey(mdLang.lang), "").orEmpty()
    private fun tryUsingFirstVolumeCover() = sourcePreferences.getBoolean(getTryUsingFirstVolumeCoverKey(mdLang.lang), false)
    private fun altTitlesInDesc() = sourcePreferences.getBoolean(getAltTitlesInDescKey(mdLang.lang), false)

    private val mangadexService by lazy {
        MangaDexService(client)
    }
    private val mangadexAuthService by lazy {
        MangaDexAuthService(baseHttpClient, headers)
    }
    private val similarService by lazy {
        SimilarService(client)
    }
    private val apiAnimeParser by lazy {
        ApiAnimeParser(mdLang.lang)
    }
    private val followsHandler by lazy {
        FollowsHandler(mdLang.lang, mangadexAuthService)
    }
    private val mangaHandler by lazy {
        AnimeHandler(mdLang.lang, mangadexService, apiAnimeParser, followsHandler)
    }
    private val similarHandler by lazy {
        SimilarHandler(mdLang.lang, mangadexService, similarService)
    }
    private val mangaPlusHandler by lazy {
        MangaPlusHandler(network.client)
    }
    private val comikeyHandler by lazy {
        ComikeyHandler(network.client, network.defaultUserAgentProvider())
    }
    private val bilibiliHandler by lazy {
        BilibiliHandler(network.client)
    }
    private val azukHandler by lazy {
        AzukiHandler(network.client, network.defaultUserAgentProvider())
    }
    private val mangaHotHandler by lazy {
        MangaHotHandler(network.client, network.defaultUserAgentProvider())
    }
    private val namicomiHandler by lazy {
        NamicomiHandler(network.client, network.defaultUserAgentProvider())
    }
    private val pageHandler by lazy {
        PageHandler(
            headers,
            mangadexService,
            mangaPlusHandler,
            comikeyHandler,
            bilibiliHandler,
            azukHandler,
            mangaHotHandler,
            namicomiHandler,
            trackPreferences,
            mdList,
        )
    }

    // UrlImportableSource methods
    override suspend fun mapUrlToAnimeUrl(uri: Uri): String? {
        val lcFirstPathSegment = uri.pathSegments.firstOrNull()?.lowercase() ?: return null

        return if (lcFirstPathSegment == "title" || lcFirstPathSegment == "anime") {
            MdUtil.buildAnimeUrl(uri.pathSegments[1])
        } else {
            null
        }
    }

    override fun mapUrlToEpisodeUrl(uri: Uri): String? {
        if (!uri.pathSegments.firstOrNull().equals("episode", true)) return null
        val id = uri.pathSegments.getOrNull(1) ?: return null
        return MdApi.episode + '/' + id
    }

    override suspend fun mapEpisodeUrlToAnimeUrl(uri: Uri): String? {
        val id = uri.pathSegments.getOrNull(1) ?: return null
        return mangaHandler.getAnimeFromEpisodeId(id)?.let { MdUtil.buildAnimeUrl(it) }
    }

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getLatestUpdates"))
    override fun fetchLatestUpdates(page: Int): Observable<AnimesPage> {
        val request = delegate.latestUpdatesRequest(page)
        val url = request.url.newBuilder()
            .removeAllQueryParameters("includeFutureUpdates")
            .build()
        return client.newCall(request.newBuilder().url(url).build())
            .asObservableSuccess()
            .map { response ->
                delegate.latestUpdatesParse(response)
            }
    }

    override suspend fun getLatestUpdates(page: Int): AnimesPage {
        val request = delegate.latestUpdatesRequest(page)
        val url = request.url.newBuilder()
            .removeAllQueryParameters("includeFutureUpdates")
            .build()

        val response = client.newCall(request.newBuilder().url(url).build()).awaitSuccess()
        return delegate.latestUpdatesParse(response)
    }

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getAnimeDetails"))
    override fun fetchAnimeDetails(anime: SAnime): Observable<SAnime> {
        return mangaHandler.fetchAnimeDetailsObservable(anime, id, coverQuality(), tryUsingFirstVolumeCover(), altTitlesInDesc())
    }

    override suspend fun getAnimeDetails(anime: SAnime): SAnime {
        return mangaHandler.getAnimeDetails(anime, id, coverQuality(), tryUsingFirstVolumeCover(), altTitlesInDesc())
    }

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getEpisodeList"))
    override fun fetchEpisodeList(anime: SAnime): Observable<List<SEpisode>> {
        return mangaHandler.fetchEpisodeListObservable(anime, blockedGroups(), blockedUploaders())
    }

    override suspend fun getEpisodeList(anime: SAnime): List<SEpisode> {
        return mangaHandler.getEpisodeList(anime, blockedGroups(), blockedUploaders())
    }

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getPageList"))
    override fun fetchPageList(episode: SEpisode): Observable<List<Page>> {
        return runAsObservable { pageHandler.fetchPageList(episode, usePort443Only(), dataSaver(), delegate) }
    }

    override suspend fun getPageList(episode: SEpisode): List<Page> {
        return pageHandler.fetchPageList(episode, usePort443Only(), dataSaver(), delegate)
    }

    override suspend fun getImage(page: Page): Response {
        val call = pageHandler.getImageCall(page)
        return call?.awaitSuccess() ?: super.getImage(page)
    }

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getImageUrl"))
    override fun fetchImageUrl(page: Page): Observable<String> {
        return pageHandler.fetchImageUrl(page) {
            @Suppress("DEPRECATION")
            super.fetchImageUrl(it)
        }
    }

    override suspend fun getImageUrl(page: Page): String {
        return pageHandler.getImageUrl(page) {
            super.getImageUrl(page)
        }
    }

    // MetadataSource methods
    override val metaClass: KClass<MangaDexSearchMetadata> = MangaDexSearchMetadata::class

    override fun newMetaInstance() = MangaDexSearchMetadata()

    override suspend fun parseIntoMetadata(metadata: MangaDexSearchMetadata, input: Triple<AnimeDto, List<String>, StatisticsAnimeDto>) {
        apiAnimeParser.parseIntoMetadata(metadata, input.first, input.second, input.third, null, coverQuality(), altTitlesInDesc())
    }

    // LoginSource methods
    override val requiresLogin: Boolean = false

    override val twoFactorAuth = LoginSource.AuthSupport.NOT_SUPPORTED

    override fun isLogged(): Boolean {
        return mdList.isLoggedIn
    }

    override fun getUsername(): String {
        return mdList.getUsername()
    }

    override fun getPassword(): String {
        return mdList.getPassword()
    }

    override suspend fun login(authCode: String): Boolean {
        return loginHelper.login(authCode)
    }

    override suspend fun logout(): Boolean {
        return loginHelper.logout()
    }

    // FollowsSource methods
    override suspend fun fetchFollows(page: Int): AnimesPage {
        return followsHandler.fetchFollows(page)
    }

    override suspend fun fetchAllFollows(): List<Pair<SAnime, MangaDexSearchMetadata>> {
        return followsHandler.fetchAllFollows()
    }

    suspend fun updateFollowStatus(animeID: String, followStatus: FollowStatus): Boolean {
        return followsHandler.updateFollowStatus(animeID, followStatus)
    }

    suspend fun fetchTrackingInfo(url: String): Track {
        return followsHandler.fetchTrackingInfo(url)
    }

    // Tracker methods
    /*suspend fun updateReadingProgress(track: Track): Boolean {
        return followsHandler.updateReadingProgress(track)
    }*/

    suspend fun updateRating(track: Track): Boolean {
        return followsHandler.updateRating(track)
    }

    suspend fun getTrackingAndAnimeInfo(track: Track): Pair<Track, MangaDexSearchMetadata?> {
        return mangaHandler.getTrackingInfo(track)
    }

    // RandomAnimeSource method
    override suspend fun fetchRandomAnimeUrl(): String {
        return mangaHandler.fetchRandomAnimeId()
    }

    suspend fun getAnimeSimilar(anime: SAnime): MetadataAnimesPage {
        return similarHandler.getSimilar(anime)
    }

    suspend fun getAnimeRelated(anime: SAnime): MetadataAnimesPage {
        return similarHandler.getRelated(anime)
    }

    suspend fun getAnimeMetadata(track: Track): SAnime? {
        return mangaHandler.getAnimeMetadata(track, id, coverQuality(), tryUsingFirstVolumeCover(), altTitlesInDesc())
    }

    companion object {
        private const val dataSaverPref = "dataSaverV5"

        fun getDataSaverPreferenceKey(dexLang: String): String {
            return "${dataSaverPref}_$dexLang"
        }

        private const val standardHttpsPortPref = "usePort443"

        fun getStandardHttpsPreferenceKey(dexLang: String): String {
            return "${standardHttpsPortPref}_$dexLang"
        }

        private const val blockedGroupsPref = "blockedGroups"

        fun getBlockedGroupsPrefKey(dexLang: String): String {
            return "${blockedGroupsPref}_$dexLang"
        }

        private const val blockedUploaderPref = "blockedUploader"

        fun getBlockedUploaderPrefKey(dexLang: String): String {
            return "${blockedUploaderPref}_$dexLang"
        }

        private const val coverQualityPref = "thumbnailQuality"

        fun getCoverQualityPrefKey(dexLang: String): String {
            return "${coverQualityPref}_$dexLang"
        }

        private const val tryUsingFirstVolumeCover = "tryUsingFirstVolumeCover"

        fun getTryUsingFirstVolumeCoverKey(dexLang: String): String {
            return "${tryUsingFirstVolumeCover}_$dexLang"
        }

        private const val altTitlesInDesc = "altTitlesInDesc"

        fun getAltTitlesInDescKey(dexLang: String): String {
            return "${altTitlesInDesc}_$dexLang"
        }
    }
}
