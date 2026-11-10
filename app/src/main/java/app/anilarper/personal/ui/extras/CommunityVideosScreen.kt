package app.anilarper.personal.ui.extras

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import app.anilarper.personal.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private data class SharedVideo(val id: String, val title: String, val category: String, val size: Long, val streamPath: String)

@Composable
fun CommunityVideosScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("anilarper_media", Context.MODE_PRIVATE) }
    var serverAddress by remember { mutableStateOf(prefs.getString("server_url", "") ?: "") }
    var connectedAddress by remember { mutableStateOf("") }
    var videos by remember { mutableStateOf<List<SharedVideo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var status by remember { mutableStateOf("Enter the secure server address to browse shared videos.") }
    var loading by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<SharedVideo?>(null) }
    val scope = rememberCoroutineScope()
    val player = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(player) { onDispose { player.release() } }

    fun connect() {
        val normalized = serverAddress.trim().trimEnd('/')
        val parsedAddress = runCatching { URL(normalized) }.getOrNull()
        if (parsedAddress?.protocol != "https" || parsedAddress.host.isBlank() ||
            parsedAddress.userInfo != null || parsedAddress.query != null || parsedAddress.ref != null) {
            status = "Enter a valid HTTPS server address without credentials, query parameters, or fragments."
            return
        }
        scope.launch {
            loading = true
            status = "Connecting…"
            try {
                val result = withContext(Dispatchers.IO) {
                    val connection = (URL("$normalized/api/videos").openConnection() as HttpURLConnection)
                    connection.connectTimeout = 10000
                    connection.readTimeout = 15000
                    connection.requestMethod = "GET"
                    connection.instanceFollowRedirects = false
                    try {
                        if (connection.responseCode !in 200..299) error("Server returned HTTP ${connection.responseCode}")
                        val body = connection.inputStream.bufferedReader().use { it.readText() }
                        val array = JSONObject(body).optJSONArray("videos") ?: org.json.JSONArray()
                        buildList {
                            for (i in 0 until array.length()) {
                                val item = array.optJSONObject(i) ?: continue
                                val id = item.optString("id")
                                val path = item.optString("streamPath")
                                if (id.isNotBlank() && path.startsWith("/media/")) {
                                    add(SharedVideo(id, item.optString("title", id.removeSuffix(".mp4")),
                                        item.optString("category", "Uncategorized").ifBlank { "Uncategorized" },
                                        item.optLong("size").coerceAtLeast(0L), path))
                                }
                            }
                        }
                    } finally { connection.disconnect() }
                }
                prefs.edit().putString("server_url", normalized).apply()
                connectedAddress = normalized
                videos = result
                status = if (result.isEmpty()) "Connected. No shared videos yet." else "Connected • ${result.size} video(s)"
            } catch (e: Exception) {
                videos = emptyList()
                status = "Couldn't connect: ${e.message ?: "Check the address and server."}"
            } finally { loading = false }
        }
    }

    Column(
        modifier.fillMaxSize().background(Ink).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("COMMUNITY", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
        Text("Shared videos", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        Text("Browse videos hosted by the Anilarper community server.", color = Muted, fontSize = 12.sp)
        OutlinedTextField(
            value = serverAddress,
            onValueChange = { serverAddress = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Secure server address (https://…)") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White, unfocusedTextColor = White,
                focusedBorderColor = Violet, unfocusedBorderColor = Muted,
                focusedLabelColor = Violet, unfocusedLabelColor = Muted,
                cursorColor = Violet
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { connect() }, enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink)) {
                Icon(Icons.Default.Cloud, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (loading) "Connecting…" else "Connect")
            }
            OutlinedButton(onClick = { if (connectedAddress.isNotBlank()) { serverAddress = connectedAddress; connect() } },
                enabled = !loading && connectedAddress.isNotBlank()) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(5.dp))
                Text("Refresh")
            }
        }
        Text(status, color = Muted, fontSize = 12.sp)
        if (selected != null) {
            Text("NOW PLAYING • ${selected!!.title}", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            AndroidView(
                factory = { PlayerView(it).apply { this.player = player; useController = true } },
                update = { it.player = player },
                modifier = Modifier.fillMaxWidth().height(210.dp).background(Color.Black, RoundedCornerShape(12.dp))
            )
        }
        Text("VIDEO LIBRARY", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        if (videos.isNotEmpty()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search shared videos") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = White, unfocusedTextColor = White,
                    focusedBorderColor = Violet, unfocusedBorderColor = Muted,
                    focusedLabelColor = Violet, unfocusedLabelColor = Muted, cursorColor = Violet
                )
            )
            val categories = listOf("All") + videos.map { it.category }.distinct().sorted()
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { category ->
                    FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Violet.copy(alpha = .2f), selectedLabelColor = Violet, labelColor = Muted))
                }
            }
        }
        val visibleVideos = videos.filter { video ->
            (selectedCategory == "All" || video.category == selectedCategory) &&
                (searchQuery.isBlank() || (video.title + " " + video.id + " " + video.category).contains(searchQuery.trim(), ignoreCase = true))
        }
        if (videos.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("Your connected server's videos will appear here.", color = Muted, fontSize = 13.sp)
            }
        } else {
            if (visibleVideos.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No videos match this search or category.", color = Muted, fontSize = 13.sp)
                }
            } else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visibleVideos, key = { it.id }) { video ->
                    Row(
                        Modifier.fillMaxWidth().animeCard(RoundedCornerShape(14.dp))
                            .clickable {
                                selected = video
                                val encodedPath = Uri.encode(video.streamPath, "/")
                                player.setMediaItem(MediaItem.fromUri("${connectedAddress}$encodedPath"))
                                player.prepare()
                                player.play()
                            }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(video.title, color = White, fontWeight = FontWeight.SemiBold, maxLines = 2)
                            Text(video.category + " • " + String.format("%.1f MB", video.size / 1048576.0), color = Muted, fontSize = 11.sp)
                        }
                        Text("PLAY", color = Violet, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
        Text("Only share videos you own or have permission to distribute.", color = Muted, fontSize = 10.sp)
    }
}
