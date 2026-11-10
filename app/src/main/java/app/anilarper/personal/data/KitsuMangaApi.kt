package app.anilarper.personal.data

import app.anilarper.personal.ReadingTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Kitsu public manga catalog provider.
 * Only general-audience (G) and parental-guidance (PG) catalog entries are requested.
 * Reading opens at Kitsu; no chapter files are copied or redistributed.
 */
object KitsuMangaApi {
    suspend fun search(query: String): List<ReadingTitle> = withContext(Dispatchers.IO) {
        val params = buildString {
            append("?page%5Blimit%5D=20&page%5Boffset%5D=0")
            append("&sort=-userCount")
            append("&filter%5BageRating%5D=G,PG")
            if (query.isNotBlank()) append("&filter%5Btext%5D=").append(URLEncoder.encode(query.trim(), "UTF-8"))
        }
        val connection = (URL("https://kitsu.io/api/edge/manga$params").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 18000
            setRequestProperty("Accept", "application/vnd.api+json")
            setRequestProperty("User-Agent", "Anilarper/1.0 (Android manga catalog)")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val raw = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) error("Kitsu returned HTTP $code")
            val data = JSONObject(raw).optJSONArray("data") ?: return@withContext emptyList()
            buildList {
                for (i in 0 until data.length()) {
                    val item = data.optJSONObject(i) ?: continue
                    val attributes = item.optJSONObject("attributes") ?: continue
                    val rating = attributes.optString("ageRating")
                    if (rating != "G" && rating != "PG") continue
                    val titles = attributes.optJSONObject("titles") ?: JSONObject()
                    val title = attributes.optString("canonicalTitle").takeIf { it.isNotBlank() }
                        ?: titles.optString("en").takeIf { it.isNotBlank() }
                        ?: titles.optString("en_jp").takeIf { it.isNotBlank() }
                        ?: continue
                    val poster = attributes.optJSONObject("posterImage") ?: JSONObject()
                    val cover = poster.optString("small").ifBlank { poster.optString("original") }
                    val slug = attributes.optString("slug")
                    val id = item.optString("id")
                    add(ReadingTitle(
                        id = id.toIntOrNull() ?: id.hashCode().let { if (it == Int.MIN_VALUE) 1 else kotlin.math.abs(it).coerceAtLeast(1) },
                        title = title,
                        nativeTitle = titles.optString("ja_jp"),
                        cover = cover,
                        format = attributes.optString("subtype", "MANGA").uppercase(),
                        status = attributes.optString("status", "UNKNOWN").replace('_', ' '),
                        chapters = attributes.optInt("chapterCount").takeIf { !attributes.isNull("chapterCount") && it > 0 },
                        volumes = attributes.optInt("volumeCount").takeIf { !attributes.isNull("volumeCount") && it > 0 },
                        description = attributes.optString("synopsis"),
                        kind = "MANGA",
                        provider = "Kitsu",
                        providerId = id,
                        sourceUrl = if (slug.isNotBlank()) "https://kitsu.io/manga/$slug" else "https://kitsu.io/manga"
                    ))
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
