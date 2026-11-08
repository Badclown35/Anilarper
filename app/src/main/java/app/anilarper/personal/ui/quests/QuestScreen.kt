package app.anilarper.personal.ui.quests

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

private data class QuestData(val id: String, val title: String, val detail: String,
    val progress: Int, val target: Int, val reward: Int)

@Composable
fun QuestScreen(context: Context, library: List<Anime>,
    readingLibrary: List<ReadingTitle>, modifier: Modifier = Modifier) {
    val prefs = remember { context.getSharedPreferences("anilarper", Context.MODE_PRIVATE) }
    var xp by remember { mutableIntStateOf(prefs.getInt("questXp", 0)) }
    var claimed by remember {
        mutableStateOf(runCatching {
            val a = JSONArray(prefs.getString("claimedQuests", "[]") ?: "[]")
            (0 until a.length()).map { a.getString(it) }.toSet()
        }.getOrDefault(emptySet()))
    }
    val quests = listOf(
        QuestData("first_save", "First addition", "Save an anime to your library.", if (library.isNotEmpty()) 1 else 0, 1, 25),
        QuestData("episode_one", "Track your progress", "Log an episode watched.", library.sumOf { it.progress }.coerceAtMost(1), 1, 30),
        QuestData("reading_one", "A new chapter", "Add a manga or light novel to your shelf.", if (readingLibrary.isNotEmpty()) 1 else 0, 1, 25),
        QuestData("collection_three", "Building a collection", "Save three anime titles.", library.size.coerceAtMost(3), 3, 50),
        QuestData("finish_one", "One story completed", "Mark an anime as completed.", if (library.any { it.watchStatus == "COMPLETED" && it.progress > 0 }) 1 else 0, 1, 60)
    )
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Spacer(Modifier.height(18.dp))
        Text("YOUR JOURNEY", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Quests & milestones", color = White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Optional goals. No streaks, deadlines, or penalties for taking a break.", color = Muted, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF342B58), Panel2)),
            RoundedCornerShape(18.dp)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("TOTAL EXPERIENCE", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(xp.toString() + " XP", color = White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Earn XP by completing milestones when you choose.", color = Muted, fontSize = 11.sp)
            }
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Violet, modifier = Modifier.size(42.dp))
        }
        quests.forEach { quest ->
            val done = quest.progress >= quest.target
            val isClaimed = claimed.contains(quest.id)
            Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(quest.title, color = White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(quest.detail, color = Muted, fontSize = 11.sp)
                    }
                    Text("+" + quest.reward + " XP", color = Violet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                LinearProgressIndicator(
                    progress = { (quest.progress.toFloat() / quest.target).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp), color = Violet, trackColor = Panel2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(quest.progress.toString() + "/" + quest.target, color = Muted, fontSize = 10.sp)
                    Spacer(Modifier.weight(1f))
                    when {
                        isClaimed -> Text("REWARD CLAIMED", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        done -> Button(onClick = {
                            xp += quest.reward
                            claimed = claimed + quest.id
                            prefs.edit().putInt("questXp", xp)
                                .putString("claimedQuests", JSONArray(claimed.toList()).toString()).apply()
                        }, colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                            Text("Claim reward", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        else -> Text("IN PROGRESS", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

