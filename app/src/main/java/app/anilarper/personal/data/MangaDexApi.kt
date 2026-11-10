package app.anilarper.personal.data

import app.anilarper.personal.ReadingTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.absoluteValue

/**
 * MangaDex catalog integration.
 *
 * Search is restricted to safe-rated manga and English-translated availability.
 * Chapters are opened on MangaDex itself; this client does not mirror or redistribute chapter pages.
 */
object MangaDexApi {
    private const val API = "https://api.mangadex.org"

    suspend fun search(query: String): List<ReadingTitle> = withContext(Dispatchers.IO) {
        val params = buildString {
            append("?limit=50&offset=0")
            append("&order[followedCount]=desc")
            append("&contentRating[]=safe")
            append("&availableTranslatedLanguage[]=en")
            append("&includes[]=cover_art")
            if (query.isNotBlank()) append("&title=").append(URLEncoder.encode(query.trim(), "UTF-8"))
        }
        val connection = (URL(API + "/manga" + params).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 18000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Anilarper/1.0 (Android manga catalog)")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val raw = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) error("MangaDex returned HTTP $code")
            val data = JSONObject(raw).optJSONArray("data") ?: return@withContext emptyList()
            buildList {
                for (i in 0 until data.length()) {
                    val item = data.optJSONObject(i) ?: continue
                    val id = item.optString("id")
                    val attributes = item.optJSONObject("attributes") ?: continue
                    val titles = attributes.optJSONObject("title") ?: JSONObject()
                    val title = titles.optString("en").takeIf { it.isNotBlank() }
                        ?: titles.keys().asSequence().mapNotNull { titles.optString(it).takeIf(String::isNotBlank) }.firstOrNull()
                        ?: continue
                    val relationships = item.optJSONArray("relationships")
                    var coverFile = ""
                    if (relationships != null) {
                        for (j in 0 until relationships.length()) {
                            val relation = relationships.optJSONObject(j) ?: continue
                            if (relation.optString("type") == "cover_art") {
                                coverFile = relation.optJSONObject("attributes")?.optString("fileName").orEmpty()
                                if (coverFile.isBlank()) {
                                    // Some responses keep the filename on the related entity.
                                    coverFile = relation.optString("id")
                                }
                                break
                            }
                        }
                    }
                    val cover = if (coverFile.isNotBlank() && !coverFile.contains("."))
                        "https://uploads.mangadex.org/covers/$id/$coverFile.256.jpg"
                    else if (coverFile.isNotBlank())
                        "https://uploads.mangadex.org/covers/$id/$coverFile.256.jpg"
                    else ""
                    val status = attributes.optString("status", "UNKNOWN").replaceFirstChar { it.uppercase() }
                    val description = attributes.optJSONObject("description")?.optString("en").orEmpty()
                    val stableId = (id.hashCode().absoluteValue.coerceAtLeast(1))
                    add(ReadingTitle(
                        id = stableId,
                        title = title,
                        nativeTitle = titles.optString("ja-ro"),
                        cover = cover,
                        format = "MANGA",
                        status = status,
                        chapters = null,
                        volumes = null,
                        description = description,
                        kind = "MANGA",
                        provider = "MangaDex",
                        providerId = id,
                        sourceUrl = "https://mangadex.org/title/$id"
                    ))
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
