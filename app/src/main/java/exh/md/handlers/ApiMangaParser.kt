package exh.md.handlers

import eu.kanade.tachiyomi.source.model.SAnime
import eu.kanade.tachiyomi.source.model.SEpisode
import exh.log.xLogE
import exh.md.dto.AnimeDto
import exh.md.dto.EpisodeDataDto
import exh.md.dto.EpisodeDto
import exh.md.dto.StatisticsAnimeDto
import exh.md.utils.MdConstants
import exh.md.utils.MdUtil
import exh.md.utils.asMdMap
import exh.metadata.metadata.MangaDexSearchMetadata
import exh.metadata.metadata.base.RaisedTag
import exh.util.capitalize
import exh.util.floor
import exh.util.nullIfEmpty
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.anime.interactor.GetFlatMetadataById
import tachiyomi.domain.anime.interactor.InsertFlatMetadata
import uy.kohesive.injekt.injectLazy
import java.util.Locale

class ApiAnimeParser(
    private val lang: String,
) {
    private val getAnime: GetAnime by injectLazy()
    private val insertFlatMetadata: InsertFlatMetadata by injectLazy()
    private val getFlatMetadataById: GetFlatMetadataById by injectLazy()

    val metaClass = MangaDexSearchMetadata::class

    /**
     * Use reflection to create a new instance of metadata
     */
    private fun newMetaInstance() = MangaDexSearchMetadata()

    suspend fun parseToAnime(
        anime: SAnime,
        sourceId: Long,
        input: AnimeDto,
        simpleEpisodes: List<String>,
        statistics: StatisticsAnimeDto?,
        coverFileName: String?,
        coverQuality: String,
        altTitlesInDesc: Boolean,
    ): SAnime {
        val animeId = getAnime.await(anime.url, sourceId)?.id
        val metadata = if (animeId != null) {
            val flatMetadata = getFlatMetadataById.await(animeId)
            flatMetadata?.raise(metaClass) ?: newMetaInstance()
        } else {
            newMetaInstance()
        }

        parseIntoMetadata(metadata, input, simpleEpisodes, statistics, coverFileName, coverQuality, altTitlesInDesc)
        if (animeId != null) {
            metadata.animeId = animeId
            insertFlatMetadata.await(metadata.flatten())
        }

        return metadata.createAnimeInfo(anime)
    }

    fun parseIntoMetadata(
        metadata: MangaDexSearchMetadata,
        animeDto: AnimeDto,
        simpleEpisodes: List<String>,
        statistics: StatisticsAnimeDto?,
        coverFileName: String?,
        coverQuality: String,
        altTitlesInDesc: Boolean,
    ) {
        with(metadata) {
            try {
                val animeAttributesDto = animeDto.data.attributes
                mdUuid = animeDto.data.id
                title = MdUtil.getTitleFromAnime(animeAttributesDto, lang)
                altTitles = animeAttributesDto.altTitles.mapNotNull { it[lang] }.nullIfEmpty()

                val animeRelationshipsDto = animeDto.data.relationships
                cover = if (!coverFileName.isNullOrEmpty()) {
                    MdUtil.cdnCoverUrl(animeDto.data.id, "$coverFileName$coverQuality")
                } else {
                    animeRelationshipsDto
                        .firstOrNull { relationshipDto -> relationshipDto.type == MdConstants.Types.coverArt }
                        ?.attributes
                        ?.fileName
                        ?.let { coverFileName ->
                            MdUtil.cdnCoverUrl(animeDto.data.id, "$coverFileName$coverQuality")
                        }
                }
                val rawDesc = MdUtil.getFromLangMap(
                    langMap = animeAttributesDto.description.asMdMap(),
                    currentLang = lang,
                    originalLanguage = animeAttributesDto.originalLanguage,
                ).orEmpty()

                description = MdUtil.cleanDescription(
                    if (altTitlesInDesc) MdUtil.addAltTitleToDesc(rawDesc, altTitles) else rawDesc,
                )

                authors = animeRelationshipsDto.filter { relationshipDto ->
                    relationshipDto.type.equals(MdConstants.Types.author, true)
                }.mapNotNull { it.attributes?.name }.distinct()

                artists = animeRelationshipsDto.filter { relationshipDto ->
                    relationshipDto.type.equals(MdConstants.Types.artist, true)
                }.mapNotNull { it.attributes?.name }.distinct()

                langFlag = animeAttributesDto.originalLanguage
                val lastEpisode = animeAttributesDto.lastEpisode?.toFloatOrNull()
                lastChapterNumber = lastEpisode?.floor()

                statistics?.rating?.let {
                    rating = it.bayesian?.toFloat()
                    // anime.users = it.users
                }

                animeAttributesDto.links?.asMdMap<String>()?.let { links ->
                    links["al"]?.let { anilistId = it }
                    links["kt"]?.let { kitsuId = it }
                    links["mal"]?.let { myAnimeListId = it }
                    links["mu"]?.let { mangaUpdatesId = it }
                    links["ap"]?.let { animePlanetId = it }
                }

                // val filteredEpisodes = filterEpisodeForChecking(networkApiAnime)

                val tempStatus = parseStatus(animeAttributesDto.status)
                val publishedOrCancelled = tempStatus == SAnime.PUBLISHING_FINISHED || tempStatus == SAnime.CANCELLED
                status = if (
                    animeAttributesDto.lastEpisode != null &&
                    publishedOrCancelled &&
                    animeAttributesDto.lastEpisode in simpleEpisodes
                ) {
                    SAnime.COMPLETED
                } else {
                    tempStatus
                }

                // things that will go with the genre tags but aren't actually genre
                val nonGenres = listOfNotNull(
                    animeAttributesDto.publicationDemographic
                        ?.let {
                            RaisedTag("Demographic", it.capitalize(Locale.US), MangaDexSearchMetadata.TAG_TYPE_DEFAULT)
                        },
                    animeAttributesDto.contentRating
                        ?.takeUnless { it == "safe" }
                        ?.let {
                            RaisedTag("Content Rating", it.capitalize(Locale.US), MangaDexSearchMetadata.TAG_TYPE_DEFAULT)
                        },
                )

                val genres = nonGenres + animeAttributesDto.tags
                    .mapNotNull {
                        it.attributes.name[lang] ?: it.attributes.name["en"]
                    }
                    .map {
                        RaisedTag("Tags", it, MangaDexSearchMetadata.TAG_TYPE_DEFAULT)
                    }

                if (tags.isNotEmpty()) tags.clear()
                tags += genres
            } catch (e: Exception) {
                xLogE("Parse into metadata error", e)
                throw e
            }
        }
    }

    /* private fun filterEpisodeForChecking(serializer: ApiAnimeSerializer): List<EpisodeSerializer> {
         serializer.data.episodes ?: return emptyList()
         return serializer.data.episodes.asSequence()
             .filter { langs.contains(it.language) }
             .filter {
                 it.episode?.let { episodeNumber ->
                     if (episodeNumber.toDoubleOrNull() == null) {
                         return@filter false
                     }
                     return@filter true
                 }
                 return@filter false
             }.toList()
     }*/

    /*private fun isOneShot(episode: EpisodeSerializer, finalEpisodeNumber: String): Boolean {
        return episode.title.equals("oneshot", true) ||
            ((episode.episode.isNullOrEmpty() || episode.episode == "0") && MdUtil.validOneShotFinalEpisodes.contains(finalEpisodeNumber))
    }*/

    private fun parseStatus(status: String?) = when (status) {
        "ongoing" -> SAnime.ONGOING
        "completed" -> SAnime.PUBLISHING_FINISHED
        "cancelled" -> SAnime.CANCELLED
        "hiatus" -> SAnime.ON_HIATUS
        else -> SAnime.UNKNOWN
    }

    fun episodeListParse(episodeListResponse: List<EpisodeDataDto>, groupMap: Map<String, String>): List<SEpisode> {
        val now = System.currentTimeMillis()
        return episodeListResponse
            .filterNot { MdUtil.parseDate(it.attributes.publishAt) > now && it.attributes.externalUrl == null }
            .map {
                mapEpisode(it, groupMap)
            }
    }

    fun episodeParseForAnimeId(episodeDto: EpisodeDto): String? {
        return episodeDto.data.relationships.find { it.type.equals("anime", true) }?.id
    }

    fun StringBuilder.appends(string: String): StringBuilder = append("$string ")

    private fun mapEpisode(
        networkEpisode: EpisodeDataDto,
        groups: Map<String, String>,
    ): SEpisode {
        val attributes = networkEpisode.attributes
        val key = MdUtil.episodeSuffix + networkEpisode.id
        val episodeName = StringBuilder()
        // Build episode name

        if (attributes.volume != null) {
            val vol = "Vol." + attributes.volume
            episodeName.appends(vol)
            // todo
            // episode.vol = vol
        }

        if (attributes.episode.isNullOrBlank().not()) {
            val chp = "Ch.${attributes.episode}"
            episodeName.appends(chp)
            // episode.episode_txt = chp
        }

        if (!attributes.title.isNullOrBlank()) {
            if (episodeName.isNotEmpty()) {
                episodeName.appends("-")
            }
            episodeName.append(attributes.title)
        }

        // if volume, episode and title is empty its a oneshot
        if (episodeName.isEmpty()) {
            episodeName.append("Oneshot")
        }
        /*if ((status == 2 || status == 3)) {
            if (finalEpisodeNumber != null) {
                if ((isOneShot(networkEpisode, finalEpisodeNumber) && totalEpisodeCount == 1) ||
                    networkEpisode.episode == finalEpisodeNumber && finalEpisodeNumber.toIntOrNull() != 0
                ) {
                    episodeName.add("[END]")
                }
            }
        }*/

        val name = episodeName.toString()
        // Convert from unix time
        val dateUpload = MdUtil.parseDate(attributes.readableAt)

        val scanlatorName = networkEpisode.relationships
            .filter {
                it.type == MdConstants.Types.scanlator
            }
            .mapNotNull { groups[it.id] }
            .map {
                if (it == "no group") {
                    "No Group"
                } else {
                    it
                }
            }
            .toSet()
            .ifEmpty { setOf("No Group") }

        val scanlator = MdUtil.getScanlatorString(scanlatorName)

        // episode.mangadex_episode_id = MdUtil.getEpisodeId(episode.url)

        // episode.language = MdLang.fromIsoCode(attributes.translatedLanguage)?.prettyPrint ?: ""

        return SEpisode(
            url = key,
            name = name,
            scanlator = scanlator,
            date_upload = dateUpload,
        )
    }
}
