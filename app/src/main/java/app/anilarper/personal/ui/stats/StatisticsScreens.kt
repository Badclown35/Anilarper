package app.anilarper.personal.ui.stats

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
import app.anilarper.personal.theme.*
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
import app.anilarper.personal.ui.music.MusicScreen
import app.anilarper.personal.ui.quests.QuestScreen
import app.anilarper.personal.ui.backup.BackupScreen
import androidx.lifecycle.ViewModelProvider
import app.anilarper.personal.data.AnimeLibraryViewModel
import app.anilarper.personal.data.ReadingLibraryViewModel
import app.anilarper.personal.data.CollectionViewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import app.anilarper.personal.*
import app.anilarper.personal.theme.*
import app.anilarper.personal.ui.home.DashboardStat

@Composable
fun WatchHistoryScreen(library: List<Anime>, modifier: Modifier = Modifier) {
    val history = library.filter { it.lastWatchedAt > 0L && it.progress > 0 }
        .sortedByDescending { it.lastWatchedAt }
    Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(18.dp))
        Text("YOUR ACTIVITY", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Watch history", color = White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Anime you've most recently updated.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        if (history.isEmpty()) {
            Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.History, contentDescription = null, tint = Violet, modifier = Modifier.size(32.dp))
                Text("Your history will appear here.", color = White, fontWeight = FontWeight.SemiBold)
                Text("Update an anime's watched episode count in your Library to start building your history.",
                    color = Muted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(history, key = { it.id }) { anime ->
                    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                        AsyncImage(model = anime.cover, contentDescription = anime.title,
                            modifier = Modifier.width(52.dp).height(70.dp).background(Panel2, RoundedCornerShape(8.dp)))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(anime.title, color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2)
                            Text("Episode ${anime.progress}${anime.episodes?.let { " / $it" } ?: ""}",
                                color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(android.text.format.DateUtils.getRelativeTimeSpanString(anime.lastWatchedAt).toString(),
                                color = Muted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsScreen(library: List<Anime>, readingLibrary: List<ReadingTitle>, modifier: Modifier = Modifier) {
    val completed = library.count { it.watchStatus == "COMPLETED" }
    val watching = library.count { it.watchStatus == "WATCHING" }
    val planning = library.count { it.watchStatus == "PLANNING" }
    val paused = library.count { it.watchStatus == "PAUSED" }
    val dropped = library.count { it.watchStatus == "DROPPED" }
    val episodes = library.sumOf { it.progress }
    val estimatedHours = episodes * 24 / 60
    val manga = readingLibrary.count { it.kind != "NOVEL" }
    val novels = readingLibrary.count { it.kind == "NOVEL" }
    val genreCounts = library.flatMap { it.genres }.groupingBy { it }.eachCount()
        .entries.sortedByDescending { it.value }.take(5)
    val total = library.size.coerceAtLeast(1)
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(7.dp))
        Text("YOUR ACTIVITY", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Your statistics", color = White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text("A clearer picture of the stories you've followed.", color = Muted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            DashboardStat("ANIME SAVED", library.size.toString(), Icons.Default.Bookmarks, Modifier.weight(1f))
            DashboardStat("EPISODES", episodes.toString(), Icons.Default.PlayCircle, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            DashboardStat("COMPLETED", completed.toString(), Icons.Default.CheckCircle, Modifier.weight(1f))
            DashboardStat("EST. HOURS", if (estimatedHours < 1) "<1" else estimatedHours.toString(), Icons.Default.Schedule, Modifier.weight(1f))
        }
        Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text("WATCH STATUS", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            val statuses = listOf(
                Triple("Watching", watching, Violet),
                Triple("Completed", completed, Color(0xFF77CFA5)),
                Triple("Planning", planning, Color(0xFF8EA8E8)),
                Triple("Paused", paused, Color(0xFFE0B66D)),
                Triple("Dropped", dropped, Color(0xFFB17C91))
            )
            statuses.forEach { (label, count, color) ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, color = White, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text("$count", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    LinearProgressIndicator(
                        progress = { (count.toFloat() / total).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(5.dp),
                        color = color, trackColor = Panel2
                    )
                }
            }
        }
        Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("YOUR READING SHELF", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                DashboardStat("MANGA", manga.toString(), Icons.Default.MenuBook, Modifier.weight(1f))
                DashboardStat("LIGHT NOVELS", novels.toString(), Icons.Default.AutoStories, Modifier.weight(1f))
            }
            Text("${readingLibrary.sumOf { it.progress }} chapters tracked • ${readingLibrary.sumOf { it.volumeProgress }} volumes tracked",
                color = Muted, fontSize = 11.sp)
        }
        if (genreCounts.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("MOST-COLLECTED GENRES", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                genreCounts.forEach { (genre, count) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(genre, color = White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text("$count ${if (count == 1) "title" else "titles"}", color = Muted, fontSize = 10.sp)
                    }
                }
            }
        }
        Text("Watch time is an estimate based on 24 minutes per episode. Statistics use only data saved on this device.",
            color = Muted, fontSize = 10.sp)
        Spacer(Modifier.height(10.dp))
    }
}

