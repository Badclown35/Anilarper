package app.anilarper.personal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AnimeRelation(val relation: String, val anime: Anime)
data class Anime(
    val id: Int,
    val title: String,
    val nativeTitle: String,
    val cover: String,
    val format: String,
    val status: String,
    val episodes: Int?,
    val score: Int?,
    val description: String,
    val watchStatus: String = "PLANNING",
    val progress: Int = 0,
    val lastWatchedAt: Long = 0L,
    val season: String = "",
    val seasonYear: Int? = null,
    val genres: List<String> = emptyList(),
    val studios: List<String> = emptyList(),
    val relations: List<AnimeRelation> = emptyList(),
    val romajiTitle: String = "",
    val synonyms: List<String> = emptyList()
)

data class AnimePage(val items: List<Anime>, val page: Int, val hasNextPage: Boolean)

data class FranchiseResult(val root: Anime, val entries: List<AnimeRelation>, val isPartial: Boolean, val scannedCount: Int)
data class AnimeFilters(
    val genre: String? = null,
    val format: String? = null,
    val status: String? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val sort: String? = null
)

object AniListApi {
    private const val ENDPOINT = "https://graphql.anilist.co"
    private const val FIELDS = """
        id
        title { romaji english native }
        synonyms
        coverImage { large }
        format status episodes averageScore
        description(asHtml: false)
        season seasonYear genres
        studios(isMain: true) { nodes { name } }
        relations {
            edges {
                relationType
                node {
                    id type format
                    title { romaji english native }
                    coverImage { large }
                    status episodes averageScore
                    description(asHtml: false)
                    season seasonYear genres
                }
            }
        }
    """

    suspend fun search(query: String, page: Int = 1, filters: AnimeFilters = AnimeFilters()): AnimePage =
        withContext(Dispatchers.IO) {
            val gql = """
                query (${'$'}search: String, ${'$'}page: Int, ${'$'}genre: [String], ${'$'}format: MediaFormat,
                    ${'$'}status: MediaStatus, ${'$'}season: MediaSeason, ${'$'}year: Int, ${'$'}sort: [MediaSort]) {
                    Page(page: ${'$'}page, perPage: 20) {
                        pageInfo { currentPage hasNextPage }
                        media(search: ${'$'}search, type: ANIME, isAdult: false,
                            genre_in: ${'$'}genre, format: ${'$'}format, status: ${'$'}status,
                            season: ${'$'}season, seasonYear: ${'$'}year, sort: ${'$'}sort) {
                            $FIELDS
                        }
                    }
                }
            """.trimIndent()
            val variables = JSONObject().put("page", page)
                .put("sort", JSONArray().put(filters.sort ?: if (query.isBlank()) "TRENDING_DESC" else "SEARCH_MATCH"))
            if (query.isNotBlank()) variables.put("search", query)
            filters.genre?.let { variables.put("genre", JSONArray().put(it)) }
            filters.format?.let { variables.put("format", it) }
            filters.status?.let { variables.put("status", it) }
            filters.season?.let { variables.put("season", it) }
            filters.seasonYear?.let { variables.put("year", it) }
            val root = request(gql, variables).getJSONObject("data").getJSONObject("Page")
            val info = root.getJSONObject("pageInfo")
            val media = root.getJSONArray("media")
            AnimePage((0 until media.length()).map { parseAnime(media.getJSONObject(it)) },
                info.optInt("currentPage", page), info.optBoolean("hasNextPage"))
        }

    suspend fun franchise(id: Int, maxTitles: Int = 80): FranchiseResult {
        val root = details(id)
        return franchise(id, root, maxTitles)
    }

    // Scan from the already available search result so a details-request failure
    // does not prevent the franchise map from loading.
    suspend fun franchise(id: Int, root: Anime, maxTitles: Int = 80): FranchiseResult =
        withContext(Dispatchers.IO) {
            val found = linkedMapOf<Int, AnimeRelation>()
            val scanned = mutableSetOf<Int>()
            var frontier = root.relations.map { it.anime.id to it.relation }.filter { it.first != id }
            var rounds = 0
            var partial = false
            while (frontier.isNotEmpty() && found.size < maxTitles && rounds < 4) {
                val next = frontier.filter { (entryId, _) ->
                    entryId != id && entryId !in scanned && entryId !in found
                }.distinctBy { it.first }.take(maxTitles - found.size)
                if (next.isEmpty()) break
                val relationById = next.toMap()
                val fetchedThisRound = mutableListOf<Anime>()
                for (batch in next.map { it.first }.chunked(20)) {
                    try {
                        fetchedThisRound += fetchAnimeBatch(batch)
                    } catch (_: Exception) {
                        // Keep already fetched entries and mark the map as partial.
                        partial = true
                    }
                }
                fetchedThisRound.forEach { anime ->
                    scanned += anime.id
                    if (anime.id != id) {
                        found.putIfAbsent(anime.id, AnimeRelation(relationById[anime.id] ?: "RELATED", anime))
                    }
                }
                frontier = fetchedThisRound.flatMap { parent ->
                    parent.relations.map { it.anime.id to it.relation }
                }
                rounds++
            }
            if (frontier.any { it.first != id && it.first !in scanned && it.first !in found }) partial = true
            if (found.size >= maxTitles) partial = true
            FranchiseResult(root, found.values.sortedWith(compareBy<AnimeRelation>(
                { franchiseOrder(it.relation) }, { it.anime.seasonYear ?: 9999 }, { it.anime.title.lowercase() }
            )), partial, scanned.size)
        }

    private fun franchiseOrder(relation: String): Int = when (relation) {
        "PREQUEL" -> 0
        "SEQUEL" -> 1
        "PARENT" -> 2
        "SIDE_STORY", "ALTERNATIVE" -> 3
        "SUMMARY", "COMPILATION" -> 4
        "SPIN_OFF" -> 5
        "CHARACTER" -> 6
        else -> 7
    }

    private suspend fun fetchAnimeBatch(ids: List<Int>): List<Anime> {
        if (ids.isEmpty()) return emptyList()
        val gql = """
            query (${'$'}ids: [Int]) {
                Page(perPage: 50) {
                    media(id_in: ${'$'}ids, type: ANIME, isAdult: false) {
                        $FIELDS
                    }
                }
            }
        """.trimIndent()
        val variables = JSONObject().put("ids", JSONArray(ids))
        val media = request(gql, variables).getJSONObject("data").getJSONObject("Page").getJSONArray("media")
        return (0 until media.length()).map { parseAnime(media.getJSONObject(it)) }
    }

    suspend fun details(id: Int): Anime = withContext(Dispatchers.IO) {
        val gql = """
            query (${'$'}id: Int) {
                Media(id: ${'$'}id, type: ANIME) {
                    $FIELDS
                }
            }
        """.trimIndent()
        parseAnime(request(gql, JSONObject().put("id", id)).getJSONObject("data").getJSONObject("Media"))
    }

    private fun request(query: String, variables: JSONObject): JSONObject {
        val body = JSONObject().put("query", query).put("variables", variables)
        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12000
            readTimeout = 18000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) {
                val message = runCatching { JSONObject(response).optJSONArray("errors")?.optJSONObject(0)?.optString("message") }.getOrNull()
                error(message?.takeIf { it.isNotBlank() } ?: "AniList returned HTTP $code")
            }
            val json = JSONObject(response)
            if (json.has("errors")) error(json.getJSONArray("errors").optJSONObject(0)?.optString("message") ?: "AniList query failed")
            return json
        } finally { connection.disconnect() }
    }

    private fun parseAnime(item: JSONObject): Anime {
        val titles = item.optJSONObject("title") ?: JSONObject()
        val title = titles.optString("english").takeIf { it.isNotBlank() }
            ?: titles.optString("romaji").ifBlank { "Untitled anime" }
        val studioNodes = item.optJSONObject("studios")?.optJSONArray("nodes") ?: JSONArray()
        val studios = (0 until studioNodes.length()).mapNotNull { studioNodes.optJSONObject(it)?.optString("name")?.takeIf(String::isNotBlank) }
        val relations = mutableListOf<AnimeRelation>()
        val edges = item.optJSONObject("relations")?.optJSONArray("edges") ?: JSONArray()
        for (i in 0 until edges.length()) {
            val edge = edges.optJSONObject(i) ?: continue
            val node = edge.optJSONObject("node") ?: continue
            if (node.optString("type") != "ANIME") continue
            relations += AnimeRelation(edge.optString("relationType"), parseAnime(node.copyWithoutRelations()))
        }
        return Anime(
            id = item.optInt("id"), title = title, nativeTitle = titles.optString("native"),
            romajiTitle = titles.optString("romaji"),
            synonyms = item.optJSONArray("synonyms").toStringList(),
            cover = item.optJSONObject("coverImage")?.optString("large").orEmpty(),
            format = item.optString("format", "UNKNOWN").replace('_', ' '),
            status = item.optString("status", "UNKNOWN").replace('_', ' '),
            episodes = item.optInt("episodes").takeIf { !item.isNull("episodes") && it > 0 },
            score = item.optInt("averageScore").takeIf { !item.isNull("averageScore") && it > 0 },
            description = cleanDescription(item.optString("description")),
            season = item.optString("season").takeIf { it != "null" }.orEmpty(),
            seasonYear = item.optInt("seasonYear").takeIf { !item.isNull("seasonYear") && it > 0 },
            genres = item.optJSONArray("genres").toStringList(),
            studios = studios,
            relations = relations
        )
    }

    private fun JSONObject.copyWithoutRelations(): JSONObject {
        val copy = JSONObject(toString())
        copy.remove("relations")
        return copy
    }

    private fun JSONArray?.toStringList(): List<String> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }

    private fun cleanDescription(value: String): String = value
        .replace(Regex("<[^>]*>"), "")
        .replace("&amp;", "&").replace("&quot;", "\"")
        .replace("&#039;", "'").replace("&lt;", "<").replace("&gt;", ">")
}

data class ReadingTitle(
    val id: Int,
    val title: String,
    val nativeTitle: String,
    val cover: String,
    val format: String,
    val status: String,
    val chapters: Int?,
    val volumes: Int?,
    val description: String,
    val kind: String,
    val progress: Int = 0,
    val volumeProgress: Int = 0,
    val readingStatus: String = "PLANNING"
)

object AniListReadingApi {
    suspend fun search(query: String, novels: Boolean): List<ReadingTitle> = withContext(Dispatchers.IO) {
        val gql = """
            query (${'$'}search: String, ${'$'}formats: [MediaFormat], ${'$'}sort: [MediaSort]) {
                Page(perPage: 20) {
                    media(search: ${'$'}search, type: MANGA, format_in: ${'$'}formats, isAdult: false, sort: ${'$'}sort) {
                        id
                        title { romaji english native }
                        coverImage { large }
                        format
                        status
                        chapters
                        volumes
                        description(asHtml: false)
                    }
                }
            }
        """.trimIndent()
        val formats = if (novels) JSONArray().put("NOVEL")
            else JSONArray().put("MANGA").put("ONE_SHOT")
        val variables = JSONObject()
            .put("formats", formats)
            .put("sort", JSONArray().put(if (query.isBlank()) "TRENDING_DESC" else "SEARCH_MATCH"))
        if (query.isNotBlank()) variables.put("search", query)
        val body = JSONObject().put("query", gql).put("variables", variables)
        val connection = (URL("https://graphql.anilist.co").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) error("AniList returned HTTP $code")
            val media = JSONObject(response).getJSONObject("data").getJSONObject("Page").getJSONArray("media")
            buildList {
                for (i in 0 until media.length()) {
                    val item = media.getJSONObject(i)
                    val titles = item.optJSONObject("title") ?: JSONObject()
                    val title = titles.optString("english").takeIf { it.isNotBlank() }
                        ?: titles.optString("romaji").ifBlank { "Untitled" }
                    add(ReadingTitle(
                        id = item.optInt("id"),
                        title = title,
                        nativeTitle = titles.optString("native"),
                        cover = item.optJSONObject("coverImage")?.optString("large").orEmpty(),
                        format = item.optString("format", "UNKNOWN").replace('_', ' '),
                        status = item.optString("status", "UNKNOWN").replace('_', ' '),
                        chapters = item.optInt("chapters").takeIf { !item.isNull("chapters") && it > 0 },
                        volumes = item.optInt("volumes").takeIf { !item.isNull("volumes") && it > 0 },
                        description = item.optString("description")
                            .replace(Regex("<[^>]*>"), "")
                            .replace("&amp;", "&").replace("&quot;", "\"")
                            .replace("&#039;", "'").replace("&lt;", "<").replace("&gt;", ">"),
                        kind = if (novels) "NOVEL" else "MANGA"
                    ))
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}