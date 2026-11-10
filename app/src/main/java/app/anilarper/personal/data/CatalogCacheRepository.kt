package app.anilarper.personal.data

import android.content.Context
import app.anilarper.personal.Anime
import app.anilarper.personal.AnimeFilters
import app.anilarper.personal.AnimePage
import app.anilarper.personal.AnimeRelation
import app.anilarper.personal.ReadingTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Small, bounded Room cache for AniList search responses used by Discover and Reading. */
class CatalogCacheRepository(context: Context) {
    private val dao = AnilarperDatabase.get(context.applicationContext).collectionDao()

    suspend fun saveAnimePage(key: String, page: AnimePage) = withContext(Dispatchers.IO) {
        dao.putCached(CatalogCacheEntity(key, JSONObject()
            .put("page", page.page)
            .put("hasNextPage", page.hasNextPage)
            .put("items", JSONArray().apply { page.items.forEach { put(it.toJson()) } })
            .toString(), System.currentTimeMillis()))
        dao.pruneCache(System.currentTimeMillis() - CACHE_MAX_AGE_MS)
    }

    suspend fun getAnimePage(key: String): AnimePage? = withContext(Dispatchers.IO) {
        val cached = dao.getCached(key) ?: return@withContext null
        runCatching {
            val json = JSONObject(cached.payload)
            val items = json.optJSONArray("items") ?: JSONArray()
            AnimePage(
                (0 until items.length()).mapNotNull { index ->
                    runCatching { items.getJSONObject(index).toAnime() }.getOrNull()
                },
                json.optInt("page", 1),
                json.optBoolean("hasNextPage")
            )
        }.getOrNull()
    }

    suspend fun saveReading(key: String, items: List<ReadingTitle>) = withContext(Dispatchers.IO) {
        dao.putCached(CatalogCacheEntity(key,
            JSONArray().apply { items.forEach { put(it.toJson()) } }.toString(),
            System.currentTimeMillis()))
        dao.pruneCache(System.currentTimeMillis() - CACHE_MAX_AGE_MS)
    }

    suspend fun getReading(key: String): List<ReadingTitle>? = withContext(Dispatchers.IO) {
        val cached = dao.getCached(key) ?: return@withContext null
        runCatching {
            val array = JSONArray(cached.payload)
            (0 until array.length()).mapNotNull { index ->
                runCatching { array.getJSONObject(index).toReadingTitle() }.getOrNull()
            }
        }.getOrNull()
    }

    companion object {
        private const val CACHE_MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000

        fun animeKey(query: String, page: Int, filters: AnimeFilters): String =
            "anime:${query.trim().lowercase()}:$page:${filters.genre}:${filters.format}:${filters.status}:${filters.season}:${filters.seasonYear}:${filters.sort}"

        fun readingKey(query: String, novels: Boolean): String =
            "reading:${if (novels) "novel" else "manga"}:${query.trim().lowercase()}"
    }
}

private fun Anime.toJson(includeRelations: Boolean = true): JSONObject =
    JSONObject()
        .put("id", id).put("title", title).put("nativeTitle", nativeTitle)
        .put("cover", cover).put("format", format).put("status", status)
        .put("episodes", episodes).put("score", score).put("description", description)
        .put("watchStatus", watchStatus).put("progress", progress).put("lastWatchedAt", lastWatchedAt)
        .put("season", season).put("seasonYear", seasonYear)
        .put("genres", JSONArray(genres)).put("studios", JSONArray(studios))
        .put("romajiTitle", romajiTitle).put("synonyms", JSONArray(synonyms)).put("malId", malId)
        .put("relations", JSONArray().apply {
            if (includeRelations) relations.forEach { relation ->
                put(JSONObject().put("relation", relation.relation).put("anime", relation.anime.toJson(false)))
            }
        })

private fun JSONObject.toAnime(): Anime {
    val relationArray = optJSONArray("relations") ?: JSONArray()
    val relations = (0 until relationArray.length()).mapNotNull { index ->
        runCatching {
            val relation = relationArray.getJSONObject(index)
            AnimeRelation(relation.optString("relation"), relation.getJSONObject("anime").toAnime())
        }.getOrNull()
    }
    val genreArray = optJSONArray("genres") ?: JSONArray()
    val studioArray = optJSONArray("studios") ?: JSONArray()
    return Anime(
        id = getInt("id"),
        title = getString("title"),
        nativeTitle = optString("nativeTitle"),
        cover = optString("cover"),
        format = optString("format"),
        status = optString("status"),
        episodes = if (isNull("episodes")) null else optInt("episodes"),
        score = if (isNull("score")) null else optInt("score"),
        description = optString("description"),
        watchStatus = optString("watchStatus", "PLANNING"),
        progress = optInt("progress"),
        lastWatchedAt = optLong("lastWatchedAt"),
        season = optString("season"),
        seasonYear = if (isNull("seasonYear")) null else optInt("seasonYear"),
        genres = (0 until genreArray.length()).mapNotNull { genreArray.optString(it).takeIf(String::isNotBlank) },
        studios = (0 until studioArray.length()).mapNotNull { studioArray.optString(it).takeIf(String::isNotBlank) },
        relations = relations,
        romajiTitle = optString("romajiTitle"),
        synonyms = (0 until (optJSONArray("synonyms")?.length() ?: 0)).mapNotNull { optJSONArray("synonyms")?.optString(it)?.takeIf(String::isNotBlank) },
        malId = if (isNull("malId")) null else optInt("malId").takeIf { it > 0 }
    )
}

private fun ReadingTitle.toJson(): JSONObject =
    JSONObject()
        .put("id", id).put("title", title).put("nativeTitle", nativeTitle)
        .put("cover", cover).put("format", format).put("status", status)
        .put("chapters", chapters).put("volumes", volumes).put("description", description)
        .put("kind", kind).put("progress", progress).put("volumeProgress", volumeProgress)
        .put("readingStatus", readingStatus)
        .put("provider", provider).put("providerId", providerId).put("sourceUrl", sourceUrl)

private fun JSONObject.toReadingTitle() = ReadingTitle(
    id = getInt("id"),
    title = getString("title"),
    nativeTitle = optString("nativeTitle"),
    cover = optString("cover"),
    format = optString("format"),
    status = optString("status"),
    chapters = if (isNull("chapters")) null else optInt("chapters"),
    volumes = if (isNull("volumes")) null else optInt("volumes"),
    description = optString("description"),
    kind = optString("kind", "MANGA"),
    progress = optInt("progress"),
    volumeProgress = optInt("volumeProgress"),
    readingStatus = optString("readingStatus", "PLANNING"),
    provider = optString("provider", "AniList"),
    providerId = optString("providerId"),
    sourceUrl = optString("sourceUrl")
)
