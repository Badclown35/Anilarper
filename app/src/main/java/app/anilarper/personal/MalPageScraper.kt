package app.anilarper.personal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException

data class MalScrapedData(
    val url: String,
    val title: String,
    val japaneseTitle: String,
    val englishTitle: String,
    val type: String,
    val episodes: String,
    val status: String,
    val aired: String,
    val premiered: String,
    val duration: String,
    val rating: String,
    val synopsis: String,
    val genres: List<String>,
    val themes: List<String>,
    val studios: List<String>,
    val producers: List<String>,
    val licensors: List<String>,
    val score: String,
    val scoredBy: String,
    val rank: String,
    val popularity: String,
    val members: String,
    val favorites: String
)

/**
 * APK-only, native Kotlin adaptation of the supplied AnimeScraper package's
 * anime-page parser. It intentionally scrapes public MAL HTML on demand.
 */
object MalPageScraper {
    suspend fun scrapeAnime(malId: Int): MalScrapedData = withContext(Dispatchers.IO) {
        require(malId > 0) { "A valid MyAnimeList ID is required." }
        val url = "https://myanimelist.net/anime/$malId"
        val doc = try {
            Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .referrer("https://www.google.com/")
                .timeout(20_000)
                .get()
        } catch (e: IOException) {
            throw IOException("MAL could not be reached: ${e.message ?: "network error"}", e)
        }
        val title = doc.selectFirst("h1.title-name")?.text()?.trim().orEmpty()
        if (title.isBlank()) {
            val titleFallback = doc.selectFirst("meta[property=og:title]")?.attr("content").orEmpty()
            if (titleFallback.isBlank()) throw IOException("MAL returned a blocked or unexpected page. Try again later.")
        }
        fun field(label: String): String {
            val node = doc.selectFirst("span.dark_text").let { _ ->
                doc.select("span.dark_text").firstOrNull { it.text().trim().removeSuffix(":") == label }
            } ?: return "N/A"
            return node.parent()?.text()?.replaceFirst(node.text(), "")?.trim()?.ifBlank { "N/A" } ?: "N/A"
        }
        fun listField(vararg labels: String): List<String> {
            val node = doc.select("span.dark_text").firstOrNull {
                it.text().trim().removeSuffix(":") in labels
            } ?: return emptyList()
            return node.parent()?.select("a")?.map { it.text().trim() }?.filter { it.isNotBlank() }.orEmpty()
        }
        fun text(selector: String) = doc.selectFirst(selector)?.text()?.trim().orEmpty()
        return@withContext MalScrapedData(
            url = url,
            title = title.ifBlank { doc.selectFirst("meta[property=og:title]")?.attr("content").orEmpty() },
            japaneseTitle = field("Japanese"),
            englishTitle = field("English"),
            type = field("Type"),
            episodes = field("Episodes"),
            status = field("Status"),
            aired = field("Aired"),
            premiered = field("Premiered"),
            duration = field("Duration"),
            rating = field("Rating"),
            synopsis = doc.selectFirst("p[itemprop=description]")?.text()?.trim().orEmpty(),
            genres = listField("Genres", "Genre"),
            themes = listField("Themes", "Theme"),
            studios = listField("Studios"),
            producers = listField("Producers"),
            licensors = listField("Licensors"),
            score = text("span[itemprop=ratingValue]"),
            scoredBy = text("span[itemprop=ratingCount]"),
            rank = text("span.numbers.ranked strong"),
            popularity = field("Popularity"),
            members = field("Members"),
            favorites = field("Favorites")
        )
    }
}
