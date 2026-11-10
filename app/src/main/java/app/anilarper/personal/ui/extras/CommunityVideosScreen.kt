package app.anilarper.personal.ui.extras

import android.app.Activity
import androidx.activity.compose.BackHandler
import android.content.Context
import android.content.pm.ActivityInfo
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
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
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import app.anilarper.personal.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private fun isPrivateLanHost(host: String): Boolean {
    if (host == "localhost" || host.endsWith(".local", ignoreCase = true)) return true
    val octets = host.split(".").mapNotNull { it.toIntOrNull() }
    if (octets.size != 4 || octets.any { it !in 0..255 }) return false
    val (a, b) = octets
    return a == 10 || a == 127 || (a == 192 && b == 168) ||
        (a == 172 && b in 16..31) || (a == 169 && b == 254)
}

private data class SharedVideo(val id: String, val title: String, val category: String, val size: Long, val streamPath: String)

@Composable
fun CommunityVideosScreen(modifier: Modifier = Modifier, onFullscreenChange: (Boolean) -> Unit = {}) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("anilarper_media", Context.MODE_PRIVATE) }
    var serverAddress by remember { mutableStateOf(prefs.getString("server_url", "https://anilarper-media-worker.dywone659.workers.dev") ?: "https://anilarper-media-worker.dywone659.workers.dev") }
    var connectedAddress by remember { mutableStateOf("") }
    var videos by remember { mutableStateOf<List<SharedVideo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var sortMode by remember { mutableStateOf("Recently added") }
    var usedBytes by remember { mutableLongStateOf(0L) }
    var pendingBytes by remember { mutableLongStateOf(0L) }
    var maxBytes by remember { mutableLongStateOf(10_000_000_000L) }
    var speedMenu by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var status by remember { mutableStateOf("Enter the secure server address to browse shared videos.") }
    var loading by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<SharedVideo?>(null) }
    var lastWatchedId by remember { mutableStateOf(prefs.getString("last_video_id", null)) }
    var fullscreen by remember { mutableStateOf(false) }
    val activity = context as? Activity
    val originalOrientation = remember(activity) { activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }

    LaunchedEffect(fullscreen) { onFullscreenChange(fullscreen) }
    BackHandler(enabled = fullscreen) { fullscreen = false }
    DisposableEffect(fullscreen, activity) {
        val window = activity?.window
        val bars = if (window != null) WindowInsetsControllerCompat(window, window.decorView) else null
        if (fullscreen) {
            bars?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            bars?.hide(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            bars?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = originalOrientation
        }
        onDispose {
            bars?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = originalOrientation
        }
    }
    val scope = rememberCoroutineScope()
    val player = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(player) { onDispose { player.release() } }

    fun playVideo(video: SharedVideo, resume: Boolean = false) {
        selected = video
        val encodedPath = Uri.encode(video.streamPath, "/")
        player.setMediaItem(MediaItem.fromUri("$connectedAddress$encodedPath"))
        player.prepare()
        if (resume && prefs.getString("last_video_id", null) == video.id) {
            player.seekTo(prefs.getLong("last_video_position", 0L).coerceAtLeast(0L))
        } else prefs.edit().putLong("last_video_position", 0L).apply()
        player.play()
    }

    DisposableEffect(player, selected) {
        val current = selected
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying && current != null && player.currentPosition > 0L) {
                    prefs.edit().putString("last_video_id", current.id)
                        .putString("last_video_title", current.title)
                        .putLong("last_video_position", player.currentPosition).apply()
                    lastWatchedId = current.id
                }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && current != null)
                    prefs.edit().putLong("last_video_position", 0L).apply()
            }
        }
        player.addListener(listener)
        onDispose {
            if (current != null && player.currentPosition > 0L) {
                prefs.edit().putString("last_video_id", current.id)
                    .putString("last_video_title", current.title)
                    .putLong("last_video_position", player.currentPosition).apply()
                lastWatchedId = current.id
            }
            player.removeListener(listener)
        }
    }

    fun connect() {
        val normalized = serverAddress.trim().trimEnd('/')
        val parsedAddress = runCatching { URL(normalized) }.getOrNull()
        if (parsedAddress == null) {
            status = "Enter a valid server URL."
            return
        }
        val secureHttps = parsedAddress.protocol == "https"
        val privateLanHttp = parsedAddress.protocol == "http" && isPrivateLanHost(parsedAddress.host)
        if ((!secureHttps && !privateLanHttp) || parsedAddress.host.isBlank() ||
            parsedAddress.userInfo != null || parsedAddress.query != null || parsedAddress.ref != null) {
            status = "Use HTTPS for remote servers, or HTTP only for a private LAN address (10.x, 192.168.x, 172.16–31.x, or .local)."
            return
        }
        scope.launch {
            loading = true
            status = "Connecting…"
            try {
                val catalogResult = withContext(Dispatchers.IO) {
                    val connection = (URL("$normalized/api/videos").openConnection() as HttpURLConnection)
                    connection.connectTimeout = 10000
                    connection.readTimeout = 15000
                    connection.requestMethod = "GET"
                    connection.instanceFollowRedirects = false
                    try {
                        if (connection.responseCode !in 200..299) error("Server returned HTTP ${connection.responseCode}")
                        val body = connection.inputStream.bufferedReader().use { it.readText() }
                        val responseJson = JSONObject(body)
                val storageJson = responseJson.optJSONObject("storage")
                val storage = Triple(storageJson?.optLong("usedBytes", 0L) ?: 0L, storageJson?.optLong("pendingBytes", 0L) ?: 0L, storageJson?.optLong("maxBytes", 10_000_000_000L) ?: 10_000_000_000L)
                val array = responseJson.optJSONArray("videos") ?: org.json.JSONArray()
                        val parsedVideos = buildList {
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
                        Pair(parsedVideos, storage)
                    } finally { connection.disconnect() }
                }
                usedBytes = catalogResult.second.first.coerceAtLeast(0L)
                pendingBytes = catalogResult.second.second.coerceAtLeast(0L)
                maxBytes = catalogResult.second.third.coerceAtLeast(1L)
                val result = catalogResult.first
                prefs.edit().putString("server_url", normalized).apply()
                connectedAddress = normalized
                videos = result
                status = (if (result.isEmpty()) "Connected. No shared videos yet." else "Connected • ${result.size} video(s)") +
                    if (privateLanHttp) " • Local network only (HTTP)" else ""
            } catch (e: Exception) {
                videos = emptyList()
                status = "Couldn't connect: ${e.message ?: "Check the address and server."}"
            } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { connect() }

    if (fullscreen && selected != null) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { PlayerView(it).apply {
                    this.player = player
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    keepScreenOn = true
                } },
                update = { it.player = player },
                modifier = Modifier.fillMaxSize()
            )
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { player.seekBack() }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                    Text("−10")
                }
                TextButton(onClick = { player.seekForward() }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                    Text("+10")
                }
                Box {
                    TextButton(onClick = { speedMenu = true }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                        Text("SPEED ${playbackSpeed}×")
                    }
                    DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                        listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                            DropdownMenuItem(text = { Text("${speed}×") }, onClick = {
                                playbackSpeed = speed
                                player.setPlaybackSpeed(speed)
                                speedMenu = false
                            })
                        }
                    }
                }
                TextButton(
                    onClick = { fullscreen = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) { Text("EXIT") }
            }
        }
    } else Column(
        modifier.fillMaxSize().background(Ink).padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("COMMUNITY", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
        Text("Shared videos", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        Text("Browse videos hosted by the Anilarper community server. Local HTTP works only on your private Wi-Fi; use HTTPS for remote sharing.", color = Muted, fontSize = 12.sp)
        OutlinedTextField(
            value = serverAddress,
            onValueChange = { serverAddress = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Server address (HTTPS or local HTTP)") },
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
        if (connectedAddress.isNotBlank()) {
            val totalUsed = (usedBytes + pendingBytes).coerceAtLeast(0L)
            val fraction = (totalUsed.toFloat() / maxBytes.toFloat()).coerceIn(0f, 1f)
            Column(Modifier.fillMaxWidth().animeCard(RoundedCornerShape(14.dp)).padding(13.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("COMMUNITY STORAGE", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.weight(1f))
                    Text(String.format("%.1f%%", fraction * 100f), color = if (fraction >= .85f) Color(0xFFFFB86B) else White,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth(),
                    color = if (fraction >= .85f) Color(0xFFFFB86B) else Violet, trackColor = Panel2)
                Text(String.format("%.2f / %.2f GB • %.2f GB pending", totalUsed / 1e9, maxBytes / 1e9, pendingBytes / 1e9),
                    color = Muted, fontSize = 10.sp)
                if (fraction >= .95f) Text("Almost full — remove unused videos before uploading more.", color = Color(0xFFFFB86B), fontSize = 11.sp)
                else if (fraction >= .85f) Text("Storage is getting low. Check the library before your next upload.", color = Color(0xFFFFB86B), fontSize = 11.sp)
            }
        }
        val resumeVideo = videos.firstOrNull { it.id == lastWatchedId }
        val savedPosition = prefs.getLong("last_video_position", 0L)
        if (resumeVideo != null && savedPosition > 0L) {
            Row(Modifier.fillMaxWidth().animeCard(RoundedCornerShape(14.dp)).clickable {
                playVideo(resumeVideo, resume = true)
            }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("CONTINUE WATCHING", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(resumeVideo.title, color = White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text("Resume at ${savedPosition / 60000L} min", color = Muted, fontSize = 11.sp)
                }
                Text("RESUME", color = Violet, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
        if (selected != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("NOW PLAYING • ${selected!!.title}", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
                TextButton(onClick = { fullscreen = true }) { Text("FULLSCREEN") }
            }
            AndroidView(
                factory = { PlayerView(it).apply {
                    this.player = player
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    keepScreenOn = true
                } },
                update = { it.player = player },
                modifier = Modifier.fillMaxWidth().height(210.dp).background(Color.Black, RoundedCornerShape(12.dp))
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { player.seekBack() }) { Text("−10 sec", color = Violet) }
                TextButton(onClick = { player.seekForward() }) { Text("+10 sec", color = Violet) }
                Spacer(Modifier.weight(1f))
                Box {
                    TextButton(onClick = { speedMenu = true }) {
                        Text("SPEED ${playbackSpeed}×", color = Violet)
                    }
                    DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                        listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                            DropdownMenuItem(text = { Text("${speed}×") }, onClick = {
                                playbackSpeed = speed; player.setPlaybackSpeed(speed); speedMenu = false
                            })
                        }
                    }
                }
            }
        }
        Text("VIDEO LIBRARY", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        if (videos.isNotEmpty()) {
            var sortExpanded by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { sortExpanded = true }) { Text("Sort: $sortMode  ⌄", color = Violet) }
                DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                    listOf("Recently added", "Title A–Z", "Largest first", "Smallest first").forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { sortMode = option; sortExpanded = false })
                    }
                }
            }
        }
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
        val filteredVideos = videos.filter { video ->
            (selectedCategory == "All" || video.category == selectedCategory) &&
                (searchQuery.isBlank() || (video.title + " " + video.id + " " + video.category).contains(searchQuery.trim(), ignoreCase = true))
        }
        val visibleVideos = when (sortMode) {
            "Title A–Z" -> filteredVideos.sortedBy { it.title.lowercase() }
            "Largest first" -> filteredVideos.sortedByDescending { it.size }
            "Smallest first" -> filteredVideos.sortedBy { it.size }
            else -> filteredVideos
        }
        if (videos.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                Text("Your connected server's videos will appear here.", color = Muted, fontSize = 13.sp)
            }
        } else {
            if (visibleVideos.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No videos match this search or category.", color = Muted, fontSize = 13.sp)
                }
            } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visibleVideos.forEach { video ->
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
