package app.anilarper.personal.ui.music

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.anilarper.personal.ui.home.HomeScreen
import app.anilarper.personal.ui.home.DashboardStat
import app.anilarper.personal.ui.reading.ReadingScreen
import app.anilarper.personal.ui.library.LibraryScreen
import app.anilarper.personal.ui.discover.DiscoverScreen
import androidx.lifecycle.ViewModelProvider
import app.anilarper.personal.data.AnimeLibraryViewModel
import app.anilarper.personal.data.ReadingLibraryViewModel
import app.anilarper.personal.data.CollectionViewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.anilarper.personal.theme.*
import coil.compose.AsyncImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import app.anilarper.personal.*
import app.anilarper.personal.LocalTrack
import app.anilarper.personal.MusicLink
import app.anilarper.personal.loadTracks
import app.anilarper.personal.persistTracks
import app.anilarper.personal.loadMusicLinks
import app.anilarper.personal.persistMusicLinks

@Composable
fun MusicScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var shuffle by remember { mutableStateOf(context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).getBoolean("musicShuffle", false)) }
    var savedTracks by remember { mutableStateOf(loadTracks(context)) }
    var tracks by remember { mutableStateOf(savedTracks.let { if (shuffle) it.shuffled() else it }) }
    var repeatOne by remember { mutableStateOf(context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).getBoolean("musicRepeatOne", false)) }
    val repeatOneLatest by rememberUpdatedState(repeatOne)
    var current by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val focusRequest = remember {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    runCatching { player?.pause() }
                    playing = false
                }
            }.build()
    }
    var message by remember { mutableStateOf<String?>(null) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkTitle by remember { mutableStateOf("") }
    var linkUrl by remember { mutableStateOf("") }
    var links by remember { mutableStateOf(loadMusicLinks(context)) }
    var preparing by remember { mutableStateOf(false) }
    fun startTrack(track: LocalTrack) {
        runCatching {
            player?.release()
            player = null
            playing = false
            preparing = true
            val next = MediaPlayer()
            next.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            next.setDataSource(context, Uri.parse(track.uri))
            next.setOnPreparedListener { prepared ->
                if (player === prepared) {
                    if (audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        prepared.start()
                        playing = true
                        message = null
                    } else {
                        playing = false
                        message = "Couldn't get audio focus."
                    }
                    preparing = false
                }
            }
            next.setOnCompletionListener { completed ->
                if (repeatOneLatest) {
                    completed.seekTo(0)
                    completed.start()
                } else {
                    val index = tracks.indexOfFirst { it.uri == track.uri }
                    val queuedNext = tracks.getOrNull(index + 1)
                    if (queuedNext != null) startTrack(queuedNext) else playing = false
                }
            }
            next.setOnErrorListener { failed, what, extra ->
                if (player === failed) {
                    preparing = false
                    playing = false
                    player = null
                    message = "This audio file couldn't be played (code $what/$extra). Try another format or re-add it."
                }
                runCatching { failed.release() }
                true
            }
            player = next
            current = track.uri
            message = "Preparing audio…"
            next.prepareAsync()
        }.onFailure {
            runCatching { player?.release() }
            preparing = false
            playing = false
            player = null
            message = it.message ?: "Unable to open this audio file."
        }
    }
    fun playAdjacent(offset: Int) {
        val index = tracks.indexOfFirst { it.uri == current }
        val target = tracks.getOrNull(index + offset)
        if (target != null) startTrack(target)
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
    val playerRef by rememberUpdatedState(player)
    DisposableEffect(Unit) { onDispose {
        playerRef?.release()
        audioManager.abandonAudioFocusRequest(focusRequest)
    } }
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
                    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).clickable {
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
                    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(13.dp)).clickable {
                        if (current == track.uri && player != null) {
                            if (preparing) {
                                message = "Audio is still preparing…"
                            } else if (playing) {
                                player?.pause()
                                playing = false
                            } else {
                                runCatching { player?.start(); playing = true }
                                    .onFailure { startTrack(track) }
                            }
                        } else {
                            startTrack(track)
                        }
                    }.padding(13.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(if (current == track.uri && playing) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = "Play " + track.title, tint = Violet, modifier = Modifier.size(30.dp))
                        Text(track.title, color = White, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 2)
                        IconButton(onClick = {
                            if (current == track.uri) { player?.release(); player = null; current = null; playing = false }
                            tracks = tracks.filterNot { it.uri == track.uri }; persistTracks(context, tracks)
                        }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Remove track", tint = Muted) }
                    }
                }
            }
            if (links.isEmpty() && tracks.isEmpty()) item {
                Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).padding(20.dp),
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
                IconButton(onClick = { playAdjacent(-1) }, enabled = tracks.indexOfFirst { it.uri == current } > 0) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous track", tint = Violet)
                }
                IconButton(onClick = {
                    if (preparing) message = "Audio is still preparing…"
                    else if (playing) { player?.pause(); playing = false }
                    else runCatching { player?.start(); playing = true }.onFailure { message = "Playback is unavailable. Try selecting the track again." }
                }, enabled = !preparing) {
                    Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play or pause", tint = Violet)
                }
                IconButton(onClick = { playAdjacent(1) }, enabled = tracks.indexOfFirst { it.uri == current } < tracks.lastIndex) {
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
