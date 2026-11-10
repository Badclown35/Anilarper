package app.anilarper.personal.ui.extras

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import app.anilarper.personal.theme.*

@Composable
fun LocalVideoPlayerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var selectedName by remember { mutableStateOf<String?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val player = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = false }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            selectedName = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected video"
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    Column(modifier.fillMaxSize().background(Ink).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("YOUR MEDIA", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
        Text("Local video player", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        Text("Play video files stored on your device. No streaming accounts or third-party sources required.",
            color = Muted, fontSize = 12.sp)
        Button(onClick = { picker.launch(arrayOf("video/*")) },
            colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink)) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(Modifier.width(7.dp))
            Text("Choose video")
        }
        Box(Modifier.fillMaxWidth().weight(1f).animeCard(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center) {
            if (selectedUri != null) {
                AndroidView(factory = { PlayerView(it).apply { this.player = player; useController = true } },
                    update = { it.player = player }, modifier = Modifier.fillMaxSize())
            } else {
                Text("Choose a video to start playback", color = Muted, fontSize = 13.sp)
            }
        }
        selectedName?.let { Text(it, color = White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1) }
    }
}
