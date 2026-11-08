package app.anilarper.personal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    val description: String
)

object AniListApi {
    suspend fun search(query: String): List<Anime> = withContext(Dispatchers.IO) {
        val gql = if (query.isBlank()) {
            """query { Page(perPage: 12) { media(type: ANIME, sort: TRENDING_DESC) { id title { romaji english native } coverImage { large } format status episodes averageScore description(asHtml: false) } } }"""
        } else {
            """query { Page(perPage: 20) { media(search: ${'$'}search, type: ANIME, sort: SEARCH_MATCH) { id title { romaji english native } coverImage { large } format status episodes averageScore description(asHtml: false) } } }"""
        }
        val body = JSONObject().put("query", gql)
        if (query.isNotBlank()) body.put("variables", JSONObject().put("search", query))
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
                        item.optInt("id"), title, titles.optString("native"),
                        item.optJSONObject("coverImage")?.optString("large").orEmpty(),
                        item.optString("format", "UNKNOWN").replace('_', ' '),
                        item.optString("status", "UNKNOWN").replace('_', ' '),
                        item.optInt("episodes").takeIf { !item.isNull("episodes") && it > 0 },
                        item.optInt("averageScore").takeIf { !item.isNull("averageScore") && it > 0 },
                        item.optString("description").replace(Regex("<[^>]*>"), "")
                            .replace("&amp;", "&").replace("&quot;", "\"")
                            .replace("&#039;", "'").replace("&lt;", "<").replace("&gt;", ">")
                    ))
                }
            }
        } finally { connection.disconnect() }
    }
}
