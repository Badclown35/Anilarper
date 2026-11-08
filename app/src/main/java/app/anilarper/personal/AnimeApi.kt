package app.anilarper.personal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
    val progress: Int = 0
)

object AniListApi {
    suspend fun search(query: String): List<Anime> = withContext(Dispatchers.IO) {
        val gql = """
            query (${'$'}search: String, ${'$'}sort: [MediaSort]) {
                Page(perPage: 20) {
                    media(search: ${'$'}search, type: ANIME, isAdult: false, sort: ${'$'}sort) {
                        id
                        title { romaji english native }
                        coverImage { large }
                        format
                        status
                        episodes
                        averageScore
                        description(asHtml: false)
                    }
                }
            }
        """.trimIndent()
        val variables = JSONObject().put("sort", JSONArray().put(if (query.isBlank()) "TRENDING_DESC" else "SEARCH_MATCH"))
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
                        ?: titles.optString("romaji").ifBlank { "Untitled anime" }
                    add(Anime(
                        id = item.optInt("id"),
                        title = title,
                        nativeTitle = titles.optString("native"),
                        cover = item.optJSONObject("coverImage")?.optString("large").orEmpty(),
                        format = item.optString("format", "UNKNOWN").replace('_', ' '),
                        status = item.optString("status", "UNKNOWN").replace('_', ' '),
                        episodes = item.optInt("episodes").takeIf { !item.isNull("episodes") && it > 0 },
                        score = item.optInt("averageScore").takeIf { !item.isNull("averageScore") && it > 0 },
                        description = item.optString("description")
                            .replace(Regex("<[^>]*>"), "")
                            .replace("&amp;", "&")
                            .replace("&quot;", "\"")
                            .replace("&#039;", "'")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                    ))
                }
            }
        } finally {
            connection.disconnect()
        }
    }
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
            query ($search: String, $formats: [MediaFormat], $sort: [MediaSort]) {
                Page(perPage: 20) {
                    media(search: $search, type: MANGA, format_in: $formats, isAdult: false, sort: $sort) {
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
