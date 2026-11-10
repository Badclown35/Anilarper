package app.anilarper.personal.ui.music

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.anilarper.personal.playback.PlaybackService
import app.anilarper.personal.theme.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executor

@Composable
fun MusicScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var shuffle by remember { mutableStateOf(context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).getBoolean("musicShuffle", false)) }
    var savedTracks by remember { mutableStateOf(loadTracks(context)) }
    var tracks by remember { mutableStateOf(savedTracks.let { if (shuffle) it.shuffled() else it }) }
    var repeatOne by remember { mutableStateOf(context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).getBoolean("musicRepeatOne", false)) }
    var current by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(false) }
    var preparing by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaController?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkTitle by remember { mutableStateOf("") }
    var linkUrl by remember { mutableStateOf("") }
    var links by remember { mutableStateOf(loadMusicLinks(context)) }

    val sessionToken = remember(context) {
        SessionToken(context, ComponentName(context, PlaybackService::class.java))
    }
    val controllerFuture = remember(sessionToken) {
        MediaController.Builder(context, sessionToken).buildAsync()
    }
    DisposableEffect(controllerFuture) {
        controllerFuture.addListener({
            runCatching { controllerFuture.get() }
                .onSuccess { player = it }
                .onFailure { message = "The audio service couldn't be started." }
        }, Executor { command -> command.run() })
        onDispose {
            MediaController.releaseFuture(controllerFuture)
        }
    }

    DisposableEffect(player) {
        val controller = player
        if (controller == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    playing = isPlaying
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    preparing = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_IDLE && controller.currentMediaItem == null) {
                        current = null
                        playing = false
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    current = mediaItem?.mediaId
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    preparing = false
                    playing = false
                    message = error.message ?: "This audio file couldn't be played."
                }
            }
            controller.addListener(listener)
            current = controller.currentMediaItem?.mediaId
            playing = controller.isPlaying
            preparing = controller.playbackState == Player.STATE_BUFFERING
            onDispose { controller.removeListener(listener) }
        }
    }

    LaunchedEffect(player, repeatOne, shuffle) {
        player?.repeatMode = if (repeatOne) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player?.shuffleModeEnabled = shuffle
    }

    fun startTrack(track: LocalTrack) {
        val controller = player
        if (controller == null) {
            message = "Connecting to the audio service…"
            return
        }
        runCatching {
            val index = tracks.indexOfFirst { it.uri == track.uri }
            require(index >= 0) { "This track is no longer in your library." }
            val queue = tracks.map { item ->
                MediaItem.Builder()
                    .setMediaId(item.uri)
                    .setUri(Uri.parse(item.uri))
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(item.title).build())
                    .build()
            }
            controller.setMediaItems(queue, index, 0L)
            controller.prepare()
            controller.play()
            message = "Preparing audio…"
        }.onFailure {
            preparing = false
            playing = false
            message = it.message ?: "Unable to open this audio file."
        }
    }

    fun playAdjacent(offset: Int) {
        val controller = player ?: return
        if (offset < 0 && controller.hasPreviousMediaItem()) controller.seekToPreviousMediaItem()
        if (offset > 0 && controller.hasNextMediaItem()) controller.seekToNextMediaItem()
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val trackName = runCatching {
                context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Audio track"
            val track = LocalTrack(uri.toString(), trackName)
            if (savedTracks.none { it.uri == track.uri }) {
                savedTracks = savedTracks + track
                tracks = if (shuffle) savedTracks.shuffled() else savedTracks
                persistTracks(context, savedTracks)
            }
            message = "Added to your local soundtrack library."
        }
    }

    Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(18.dp))
        Text("ANILARPER AUDIO", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Your soundtrack hub.", color = White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Keep your favorite listening sources together.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showLinkDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink),
                shape = RoundedCornerShape(13.dp), modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Link, contentDescription = null)
                Spacer(Modifier.width(5.dp)); Text("Add music link", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = { picker.launch(arrayOf("audio/*")) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Violet),
                shape = RoundedCornerShape(13.dp), modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.AudioFile, contentDescription = null)
                Spacer(Modifier.width(5.dp)); Text("Add local file")
            }
        }
        message?.let { Text(it, color = Violet, fontSize = 11.sp) }
        if (tracks.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().background(Panel2, RoundedCornerShape(13.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PLAYBACK OPTIONS", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        shuffle = !shuffle
                        context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).edit().putBoolean("musicShuffle", shuffle).apply()
                        tracks = if (shuffle) savedTracks.shuffled() else savedTracks
                    }) { Icon(Icons.Default.Shuffle, contentDescription = null, tint = if (shuffle) Violet else Muted); Text("Shuffle", color = if (shuffle) Violet else Muted, fontSize = 10.sp) }
                    TextButton(onClick = {
                        repeatOne = !repeatOne
                        context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).edit().putBoolean("musicRepeatOne", repeatOne).apply()
                    }) { Icon(Icons.Default.RepeatOne, contentDescription = null, tint = if (repeatOne) Violet else Muted); Text("Repeat one", color = if (repeatOne) Violet else Muted, fontSize = 10.sp) }
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp),
            contentPadding = PaddingValues(bottom = 20.dp), modifier = Modifier.weight(1f)) {
            if (links.isNotEmpty()) {
                item { Text("STREAMING LINKS", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                items(links, key = { it.url }) { link ->
                    Row(Modifier.fillMaxWidth().animeCard(RoundedCornerShape(14.dp)).clickable {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url))) }
                            .onFailure { message = "Couldn't open this music link." }
                    }.padding(13.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Violet, modifier = Modifier.size(26.dp))
                        Column(Modifier.weight(1f)) {
                            Text(link.title, color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2)
                            Text(link.source, color = Muted, fontSize = 10.sp)
                        }
                        IconButton(onClick = {
                            links = links.filterNot { it.url == link.url }; persistMusicLinks(context, links)
                        }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Remove link", tint = Muted) }
                    }
                }
            }
            if (tracks.isNotEmpty()) {
                item { Text("LOCAL AUDIO FILES", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                items(tracks, key = { it.uri }) { track ->
                    Row(Modifier.fillMaxWidth().animeCard(RoundedCornerShape(13.dp)).clickable {
                        val controller = player
                        if (current == track.uri && controller != null) {
                            if (preparing) message = "Audio is still preparing…"
                            else if (playing) controller.pause() else controller.play()
                        } else startTrack(track)
                    }.padding(13.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(if (current == track.uri && playing) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = "Play " + track.title, tint = Violet, modifier = Modifier.size(30.dp))
                        Text(track.title, color = White, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 2)
                        IconButton(onClick = {
                            val index = tracks.indexOfFirst { it.uri == track.uri }
                            if (index >= 0 && player?.mediaItemCount?.let { index < it } == true) {
                                player?.removeMediaItem(index)
                            }
                            if (current == track.uri && player?.mediaItemCount == 0) {
                                player?.stop()
                                current = null
                                playing = false
                            }
                            tracks = tracks.filterNot { it.uri == track.uri }
                            savedTracks = savedTracks.filterNot { it.uri == track.uri }
                            persistTracks(context, savedTracks)
                        }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Remove track", tint = Muted) }
                    }
                }
            }
            if (links.isEmpty() && tracks.isEmpty()) item {
                Column(Modifier.fillMaxWidth().animeCard(RoundedCornerShape(18.dp)).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Violet, modifier = Modifier.size(34.dp))
                    Text("Your music shelf is empty.", color = White, fontWeight = FontWeight.SemiBold)
                    Text("Add a YouTube, SoundCloud, Spotify, Apple Music, or other listening link—or add an audio file stored on your phone.",
                        color = Muted, fontSize = 12.sp)
                }
            }
        }
        if (current != null) {
            Row(Modifier.fillMaxWidth().background(Panel2, RoundedCornerShape(14.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (preparing) "PREPARING · LOCAL AUDIO" else "NOW PLAYING · LOCAL", color = Violet, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(tracks.firstOrNull { it.uri == current }?.title ?: "Audio track", color = White, fontSize = 12.sp, maxLines = 1)
                }
                IconButton(onClick = { playAdjacent(-1) }, enabled = player?.hasPreviousMediaItem() == true) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous track", tint = Violet)
                }
                IconButton(onClick = {
                    if (preparing) message = "Audio is still preparing…"
                    else if (playing) player?.pause() else player?.play()
                }, enabled = !preparing && player != null) {
                    Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play or pause", tint = Violet)
                }
                IconButton(onClick = { playAdjacent(1) }, enabled = player?.hasNextMediaItem() == true) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next track", tint = Violet)
                }
            }
        }
    }
    if (showLinkDialog) {
        AlertDialog(onDismissRequest = { showLinkDialog = false }, containerColor = Panel2,
            title = { Text("Add a listening link", color = White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(linkTitle, { linkTitle = it.take(70) }, singleLine = true,
                        label = { Text("Name") }, placeholder = { Text("Anime OST playlist") })
                    OutlinedTextField(linkUrl, { linkUrl = it.take(1000) }, singleLine = true,
                        label = { Text("Music URL") }, placeholder = { Text("https://…") })
                    Text("Opens through the service's official app or website. Anilarper doesn't download or re-host streams.",
                        color = Muted, fontSize = 10.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = runCatching { Uri.parse(linkUrl.trim()) }.getOrNull()
                    if (parsed != null && parsed.scheme == "https" && !parsed.host.isNullOrBlank()) {
                        val host = parsed.host!!.lowercase()
                        val source = when {
                            host.contains("youtube.com") || host == "youtu.be" -> "YouTube"
                            host.contains("soundcloud.com") -> "SoundCloud"
                            host.contains("spotify.com") -> "Spotify"
                            host.contains("music.apple.com") -> "Apple Music"
                            host.contains("music.youtube.com") -> "YouTube Music"
                            else -> host.removePrefix("www.")
                        }
                        val item = MusicLink(linkTitle.ifBlank { source + " link" }, parsed.toString(), source)
                        if (links.none { it.url == item.url }) { links = links + item; persistMusicLinks(context, links) }
                        linkTitle = ""; linkUrl = ""; showLinkDialog = false; message = "Listening link saved."
                    } else message = "Enter a valid HTTPS music link."
                }) { Text("Save link", color = Violet) }
            },
            dismissButton = { TextButton(onClick = { showLinkDialog = false }) { Text("Cancel", color = Muted) } }
        )
    }
}

private data class LocalTrack(val uri: String, val title: String)
private data class MusicLink(val title: String, val url: String, val source: String)

private fun <T> JSONArray.mapSafe(block: (JSONObject) -> T): List<T> =
    (0 until length()).mapNotNull { index -> runCatching { block(getJSONObject(index)) }.getOrNull() }

private fun readJsonArray(context: Context, key: String): JSONArray? {
    val prefs = context.getSharedPreferences("anilarper", Context.MODE_PRIVATE)
    val raw = prefs.getString(key, "[]") ?: "[]"
    return runCatching { JSONArray(raw) }.getOrElse {
        prefs.edit().putString("${key}_corrupt_backup", raw).apply()
        null
    }
}

private fun loadTracks(context: Context): List<LocalTrack> =
    readJsonArray(context, "tracks")?.mapSafe { item ->
        LocalTrack(item.getString("uri"), item.optString("title", "Audio track").ifBlank { "Audio track" })
    } ?: emptyList()

private fun persistTracks(context: Context, tracks: List<LocalTrack>) {
    val array = JSONArray()
    tracks.forEach { array.put(JSONObject().put("uri", it.uri).put("title", it.title)) }
    context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).edit().putString("tracks", array.toString()).apply()
}

private fun loadMusicLinks(context: Context): List<MusicLink> =
    readJsonArray(context, "musicLinks")?.mapSafe { o ->
        MusicLink(o.getString("title"), o.getString("url"), o.getString("source"))
    } ?: emptyList()

private fun persistMusicLinks(context: Context, links: List<MusicLink>) {
    val array = JSONArray()
    links.forEach { array.put(JSONObject().put("title", it.title).put("url", it.url).put("source", it.source)) }
    context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).edit().putString("musicLinks", array.toString()).apply()
}
