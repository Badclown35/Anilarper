package app.anilarper.personal.ui.music

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.anilarper.personal.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private data class OnlineMusicResult(
    val title: String,
    val url: String,
    val license: String,
    val artist: String
)

@Composable
internal fun OnlineMusicDiscovery(
    onPlay: (String, String) -> Unit,
    onSave: (String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<OnlineMusicResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Search freely licensed audio from Wikimedia Commons, or browse YouTube Music.") }

    fun openSearch(base: String) {
        val term = query.trim().ifBlank { "anime soundtrack" }
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(base + Uri.encode(term)))) }
            .onFailure { status = "Couldn't open that music service." }
    }

    fun search() {
        val term = query.trim()
        if (term.isBlank()) {
            status = "Enter an artist, track, anime, or game soundtrack."
            return
        }
        scope.launch {
            loading = true
            status = "Searching freely licensed audio…"
            try {
                val found = withContext(Dispatchers.IO) {
                    val encoded = URLEncoder.encode(term, "UTF-8")
                    val endpoint = "https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch=$encoded&gsrnamespace=6&gsrlimit=30&prop=imageinfo&iiprop=url%7Cmime%7Cextmetadata&format=json"
                    val connection = (URL(endpoint).openConnection() as HttpURLConnection)
                    connection.connectTimeout = 10000
                    connection.readTimeout = 15000
                    connection.setRequestProperty("User-Agent", "AnilarperAndroid/1.0 (music discovery)")
                    try {
                        if (connection.responseCode !in 200..299) error("Search service returned HTTP ${connection.responseCode}")
                        val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                        val pages = root.optJSONObject("query")?.optJSONObject("pages") ?: JSONObject()
                        buildList {
                            val keys = pages.keys()
                            while (keys.hasNext()) {
                                val page = pages.optJSONObject(keys.next()) ?: continue
                                val info = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
                                val mime = info.optString("mime")
                                val url = info.optString("url")
                                if (!mime.startsWith("audio/") || !url.startsWith("https://")) continue
                                val metadata = info.optJSONObject("extmetadata") ?: JSONObject()
                                fun meta(name: String): String = metadata.optJSONObject(name)?.optString("value").orEmpty()
                                    .replace(Regex("<[^>]*>"), " ").replace("&nbsp;", " ").trim()
                                val license = meta("LicenseShortName")
                                val licenseUrl = meta("LicenseUrl")
                                val normalizedLicense = license.lowercase()
                                val isOpenLicense = normalizedLicense.contains("cc") ||
                                    normalizedLicense.contains("creative commons") ||
                                    normalizedLicense.contains("public domain") ||
                                    normalizedLicense == "pd"
                                if (!isOpenLicense || licenseUrl.isBlank()) continue
                                val artist = meta("Artist").ifBlank { "Unknown artist" }
                                add(OnlineMusicResult(
                                    page.optString("title").removePrefix("File:").ifBlank { "Untitled audio" },
                                    url, license, artist
                                ))
                            }
                        }
                    } finally {
                        connection.disconnect()
                    }
                }
                results = found
                status = if (found.isEmpty()) "No openly licensed audio matched that search. Try a different title or use YouTube Music." else "${found.size} freely licensed audio result(s)."
            } catch (error: Exception) {
                results = emptyList()
                status = "Search failed: ${error.message ?: "Please try again."}"
            } finally {
                loading = false
            }
        }
    }

    Column(
        Modifier.fillMaxWidth().background(Panel2, RoundedCornerShape(18.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text("DISCOVER MUSIC", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text("Find soundtracks online", color = White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text("Search Creative Commons and public-domain audio that Anilarper can stream directly.", color = Muted, fontSize = 11.sp)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Song, artist, anime, or game OST") },
            trailingIcon = {
                IconButton(onClick = { search() }, enabled = !loading) {
                    Icon(Icons.Default.Search, contentDescription = "Search music", tint = Violet)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White, unfocusedTextColor = White,
                focusedBorderColor = Violet, unfocusedBorderColor = Muted,
                focusedLabelColor = Violet, unfocusedLabelColor = Muted, cursorColor = Violet
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { search() }, enabled = !loading && query.isNotBlank(),
                modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink)) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(Modifier.width(5.dp))
                Text(if (loading) "Searching…" else "Search free audio")
            }
            OutlinedButton(onClick = {
                val target = Uri.parse("https://music.youtube.com/search?q=" + Uri.encode(query.ifBlank { "anime soundtrack" }))
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, target)) }
                    .onFailure { status = "Couldn't open YouTube Music." }
            }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.OpenInNew, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("YouTube Music")
            }
        }
        Text(status, color = Muted, fontSize = 10.sp)
        Text("DISCOVER ON OTHER SERVICES", color = Violet, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { openSearch("https://archive.org/search?query=") }, modifier = Modifier.weight(1f)) { Text("Archive.org") }
            OutlinedButton(onClick = { openSearch("https://soundcloud.com/search/sounds?q=") }, modifier = Modifier.weight(1f)) { Text("SoundCloud") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { openSearch("https://bandcamp.com/search?q=") }, modifier = Modifier.weight(1f)) { Text("Bandcamp") }
            OutlinedButton(onClick = { openSearch("https://www.jamendo.com/search?q=") }, modifier = Modifier.weight(1f)) { Text("Jamendo") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { openSearch("https://open.spotify.com/search/") }, modifier = Modifier.weight(1f)) { Text("Spotify") }
            OutlinedButton(onClick = { openSearch("https://music.apple.com/us/search?term=") }, modifier = Modifier.weight(1f)) { Text("Apple Music") }
        }
        if (results.isNotEmpty()) {
            Text("OPEN-LICENSE STREAMS", color = Violet, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            results.forEach { result ->
                Row(
                    Modifier.fillMaxWidth().background(Ink.copy(alpha = .55f), RoundedCornerShape(12.dp)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Violet)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(result.title, color = White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        Text(result.artist, color = Muted, fontSize = 9.sp, maxLines = 1)
                        Text(result.license, color = Muted, fontSize = 8.sp, maxLines = 1)
                    }
                    IconButton(onClick = { onPlay(result.url, result.title) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play ${result.title}", tint = Violet)
                    }
                    IconButton(onClick = { onSave(result.url, result.title) }) {
                        Icon(Icons.Default.Add, contentDescription = "Save ${result.title}", tint = Violet)
                    }
                }
            }
        }
        Text("YouTube Music opens in its official app or website. Direct background playback is available for the openly licensed audio results.", color = Muted, fontSize = 9.sp)
    }
}
