package app.anilarper.personal

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AnilarperApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnilarperApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: "Home"
    var moreOpen by remember { mutableStateOf(false) }
    val activity = context as ComponentActivity
    val collectionFactory = remember(context) { CollectionViewModelFactory(context.applicationContext) }
    val animeViewModel = remember(activity) {
        ViewModelProvider(activity, collectionFactory)[AnimeLibraryViewModel::class.java]
    }
    val readingViewModel = remember(activity) {
        ViewModelProvider(activity, collectionFactory)[ReadingLibraryViewModel::class.java]
    }
    val library by animeViewModel.library.collectAsState()
    val readingLibrary by readingViewModel.library.collectAsState()
    LaunchedEffect(Unit) {
        val accent = context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).getInt("profileAccent", 0)
        VioletState.value = AccentPalette[accent.coerceIn(0, AccentPalette.lastIndex)]
    }
    AnilarperTheme {
        Scaffold(
            containerColor = Ink,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF11131B), tonalElevation = 0.dp) {
                    listOf("Home" to Icons.Default.Home, "Discover" to Icons.Default.Explore,
                        "Library" to Icons.Default.Bookmarks, "More" to Icons.Default.MoreHoriz).forEach { (label, icon) ->
                        val isSelected = if (label == "More")
                            currentRoute in setOf("Read", "Music", "Quests", "Profile", "Stats", "Backup", "History")
                        else currentRoute == label
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (label == "More") moreOpen = true
                                else navController.navigate(label) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label,
                                modifier = Modifier.size(if (isSelected) 23.dp else 21.dp)) },
                            label = { Text(label, fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, maxLines = 1) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Violet, selectedTextColor = Violet,
                                indicatorColor = Violet.copy(alpha = .14f),
                                unselectedIconColor = Color(0xFF777B8D),
                                unselectedTextColor = Color(0xFF777B8D)
                            )
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(navController = navController, startDestination = "Home", modifier = Modifier.padding(padding)) {
                composable("Home") {
                    HomeScreen(Modifier.fillMaxSize(), library,
                        onDiscover = { navController.navigate("Discover") { launchSingleTop = true } },
                        onOpenLibrary = { navController.navigate("Library") { launchSingleTop = true } },
                        onOpenRead = { navController.navigate("Read") { launchSingleTop = true } },
                        onOpenQuests = { navController.navigate("Quests") { launchSingleTop = true } },
                        onOpenStats = { navController.navigate("Stats") { launchSingleTop = true } },
                        onOpenProfile = { navController.navigate("Profile") { launchSingleTop = true } })
                }
                composable("Discover") {
                    DiscoverScreen(Modifier.fillMaxSize(), library.map { it.id }.toSet()) { anime ->
                        animeViewModel.toggle(anime)
                    }
                }
                composable("Library") {
                    LibraryScreen(Modifier.fillMaxSize(), library,
                        onRemove = { animeViewModel.remove(it) },
                        onUpdate = { animeViewModel.update(it) })
                }
                composable("Read") {
                    ReadingScreen(Modifier.fillMaxSize(), readingLibrary,
                        onAdd = { readingViewModel.add(it) },
                        onUpdate = { readingViewModel.update(it) },
                        onRemove = { readingViewModel.remove(it) })
                }
                composable("Music") { MusicScreen(Modifier.fillMaxSize()) }
                composable("Quests") { QuestScreen(context, library, readingLibrary, Modifier.fillMaxSize()) }
                composable("Stats") { StatisticsScreen(library, readingLibrary, Modifier.fillMaxSize()) }
                composable("History") { WatchHistoryScreen(library, Modifier.fillMaxSize()) }
                composable("Backup") {
                    BackupScreen(context, Modifier.fillMaxSize(), onRestored = {
                        animeViewModel.refresh()
                        readingViewModel.refresh()
                        val accent = context.getSharedPreferences("anilarper", Context.MODE_PRIVATE)
                            .getInt("profileAccent", 0)
                        VioletState.value = AccentPalette[accent.coerceIn(0, AccentPalette.lastIndex)]
                    })
                }
                composable("Profile") { ProfileScreen(context, library, readingLibrary, Modifier.fillMaxSize()) }
            }
        }
        if (moreOpen) {
            ModalBottomSheet(onDismissRequest = { moreOpen = false }, containerColor = Panel) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MORE FROM ANILARPER", color = Violet, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    listOf(
                        Triple("Read", "Manga & light novels", Icons.Default.MenuBook),
                        Triple("Music", "Your soundtrack sources", Icons.Default.LibraryMusic),
                        Triple("Quests", "Milestones & XP", Icons.Default.EmojiEvents),
                        Triple("Stats", "Your anime and reading activity", Icons.Default.Insights),
                        Triple("History", "Recently updated episodes", Icons.Default.History),
                        Triple("Backup", "Export or restore your collection", Icons.Default.Backup),
                        Triple("Profile", "Your personal space", Icons.Default.Person)
                    ).forEach { (label, subtitle, icon) ->
                        Row(Modifier.fillMaxWidth().clickable { navController.navigate(label) { launchSingleTop = true }; moreOpen = false }
                            .background(Panel2, RoundedCornerShape(14.dp)).padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                            Icon(icon, contentDescription = null, tint = Violet)
                            Column(Modifier.weight(1f)) {
                                Text(label, color = White, fontWeight = FontWeight.SemiBold)
                                Text(subtitle, color = Muted, fontSize = 11.sp)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Muted)
                        }
                    }
                }
            }
        }
    }
}


internal data class MusicLink(val title: String, val url: String, val source: String)

internal fun loadMusicLinks(context: Context): List<MusicLink> =
    readJsonArray(context, "musicLinks")?.mapSafe { o ->
        MusicLink(o.getString("title"), o.getString("url"), o.getString("source"))
    } ?: emptyList()

internal fun persistMusicLinks(context: Context, links: List<MusicLink>) {
    val a = JSONArray()
    links.forEach { a.put(JSONObject().put("title", it.title).put("url", it.url).put("source", it.source)) }
    context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).edit().putString("musicLinks", a.toString()).apply()
}

private data class ProfileSettings(
    val displayName: String = "Anime Explorer",
    val bio: String = "Finding my next favorite story.",
    val avatarUrl: String = "",
    val bannerStyle: Int = 0,
    val favoriteTitles: String = "",
    val accentStyle: Int = 0
)

private fun loadProfile(context: Context): ProfileSettings {
    val p = context.getSharedPreferences("anilarper", Context.MODE_PRIVATE)
    return ProfileSettings(
        p.getString("profileName", "Anime Explorer") ?: "Anime Explorer",
        p.getString("profileBio", "Finding my next favorite story.") ?: "",
        p.getString("profileAvatar", "") ?: "",
        p.getInt("profileBanner", 0),
        p.getString("profileFavorites", "") ?: "",
        p.getInt("profileAccent", 0)
    )
}

private fun saveProfile(context: Context, profile: ProfileSettings) {
    context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).edit()
        .putString("profileName", profile.displayName)
        .putString("profileBio", profile.bio)
        .putString("profileAvatar", profile.avatarUrl)
        .putInt("profileBanner", profile.bannerStyle)
        .putString("profileFavorites", profile.favoriteTitles)
        .putInt("profileAccent", profile.accentStyle)
        .apply()
}

@Composable
private fun BackupScreen(context: Context, modifier: Modifier = Modifier, onRestored: () -> Unit = {}) {
    var message by remember { mutableStateOf<String?>(null) }
    var confirmRestore by remember { mutableStateOf<Uri?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val prefs = context.getSharedPreferences("anilarper", Context.MODE_PRIVATE)
            val backup = JSONObject()
                .put("format", "anilarper-backup")
                .put("version", 1)
                .put("preferences", JSONObject(prefs.all))
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(backup.toString(2)) }
                    ?: error("Couldn't open the backup file for writing.")
            }.onSuccess { message = "Backup exported successfully." }
                .onFailure { message = it.message ?: "Backup export failed." }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) confirmRestore = uri
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("YOUR DATA", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Backup & restore", color = White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Keep a portable copy of your Anilarper settings and collection.", color = Muted, fontSize = 12.sp)
        Button(onClick = { export.launch("anilarper-backup.json") },
            colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink),
            modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.FileDownload, contentDescription = null)
            Spacer(Modifier.width(8.dp)); Text("Export backup")
        }
        OutlinedButton(onClick = { import.launch(arrayOf("application/json", "text/*")) },
            modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Violet)) {
            Icon(Icons.Default.Restore, contentDescription = null)
            Spacer(Modifier.width(8.dp)); Text("Restore from backup")
        }
        Text("Restoring replaces the app's locally saved data on this device. Export a fresh backup first if you want to keep the current state.",
            color = Muted, fontSize = 11.sp)
        message?.let { Text(it, color = Violet, fontSize = 12.sp) }
    }
    confirmRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmRestore = null },
            containerColor = Panel2,
            title = { Text("Replace local data?", color = White) },
            text = { Text("This will replace your current Anilarper settings, library, progress, quests, and music links with the selected backup.", color = Muted) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = null
                    runCatching {
                        val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            ?: error("Couldn't read the selected file.")
                        val backup = JSONObject(raw)
                        require(backup.optString("format") == "anilarper-backup") { "This isn't an Anilarper backup file." }
                        require(backup.optInt("version", 0) == 1) { "Unsupported backup version." }
                        val values = backup.getJSONObject("preferences")
                        require(values.length() > 0) { "This backup contains no data. Nothing was changed." }
                        val prefs = context.getSharedPreferences("anilarper", Context.MODE_PRIVATE)
                        val snapshot = JSONObject().put("format", "anilarper-backup").put("version", 1)
                            .put("preferences", JSONObject(prefs.all))
                        java.io.File(context.filesDir, "pre-restore-backup.json").writeText(snapshot.toString())
                        val editor = prefs.edit().clear()
                        val keys = values.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            when (val value = values.get(key)) {
                                is String -> editor.putString(key, value)
                                is Boolean -> editor.putBoolean(key, value)
                                is Int -> editor.putInt(key, value)
                                is Long -> editor.putLong(key, value)
                                is Double -> editor.putFloat(key, value.toFloat())
                                is Number -> editor.putLong(key, value.toLong())
                                is JSONArray, is JSONObject -> editor.putString(key, value.toString())
                            }
                        }
                        check(editor.commit()) { "Couldn't save the restored data." }
                    }.onSuccess { onRestored(); message = "Backup restored." }
                        .onFailure { message = it.message ?: "Restore failed. Your data may not have changed." }
                }) { Text("Restore", color = Violet) }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = null }) { Text("Cancel", color = Muted) } }
        )
    }
}

@Composable
private fun WatchHistoryScreen(library: List<Anime>, modifier: Modifier = Modifier) {
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
private fun StatisticsScreen(library: List<Anime>, readingLibrary: List<ReadingTitle>, modifier: Modifier = Modifier) {
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

@Composable
private fun ProfileScreen(context: Context, library: List<Anime>,
    readingLibrary: List<ReadingTitle>, modifier: Modifier = Modifier) {
    var profile by remember { mutableStateOf(loadProfile(context)) }
    var editing by remember { mutableStateOf(false) }
    val bannerColors = listOf(
        listOf(Color(0xFF30294F), Color(0xFF171923), Color(0xFF252039)),
        listOf(Color(0xFF193D45), Color(0xFF171923), Color(0xFF24334A)),
        listOf(Color(0xFF4A283D), Color(0xFF171923), Color(0xFF34253F)),
        listOf(Color(0xFF343A45), Color(0xFF171923), Color(0xFF242B39))
    )
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(18.dp))
        Text("YOUR SPACE", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(bannerColors[profile.bannerStyle.coerceIn(0, 3)]),
            RoundedCornerShape(22.dp)).padding(18.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (profile.avatarUrl.isNotBlank()) {
                        AsyncImage(model = profile.avatarUrl, contentDescription = "Profile avatar",
                            modifier = Modifier.size(68.dp).background(Panel2, CircleShape))
                    } else {
                        Box(Modifier.size(68.dp).background(Panel2, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Violet, modifier = Modifier.size(34.dp))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(profile.displayName, color = White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("ANILARPER MEMBER", color = Violet, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    }
                }
                Text(profile.bio.ifBlank { "No bio yet. Make this space yours." }, color = White.copy(alpha = .85f), fontSize = 12.sp)
                OutlinedButton(onClick = { editing = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Violet.copy(alpha = .6f))) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Customize profile")
                }
            }
        }
        Text("YOUR COLLECTION", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ProfileStat("Anime", library.size.toString(), Modifier.weight(1f))
            ProfileStat("Completed", library.count { it.watchStatus == "COMPLETED" }.toString(), Modifier.weight(1f))
            ProfileStat("Reading", readingLibrary.size.toString(), Modifier.weight(1f))
        }
        Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("FAVORITES", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(profile.favoriteTitles.ifBlank { "Pin your favorite anime, manga, or light novels in Customize profile." },
                color = if (profile.favoriteTitles.isBlank()) Muted else White, fontSize = 13.sp)
        }
        Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(17.dp)).padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ACHIEVEMENT SHOWCASE", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            val achievements = listOf(
                Triple("Collector", "Save your first anime", library.isNotEmpty()),
                Triple("First finale", "Complete an anime", library.any { it.watchStatus == "COMPLETED" }),
                Triple("Binge tracker", "Log 10 episodes", library.sumOf { it.progress } >= 10),
                Triple("Bookworm", "Add a reading title", readingLibrary.isNotEmpty())
            )
            achievements.forEach { (name, description, unlocked) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(34.dp).background(if (unlocked) Violet.copy(alpha = .16f) else Panel2, CircleShape),
                        contentAlignment = Alignment.Center) {
                        Icon(if (unlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                            contentDescription = null, tint = if (unlocked) Violet else Muted, modifier = Modifier.size(17.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(name, color = if (unlocked) White else Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(description, color = Muted, fontSize = 10.sp)
                    }
                    Text(if (unlocked) "UNLOCKED" else "LOCKED",
                        color = if (unlocked) Violet else Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text("Profile settings are saved on this device.", color = Muted, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
    }
    if (editing) {
        var name by remember(profile) { mutableStateOf(profile.displayName) }
        var bio by remember(profile) { mutableStateOf(profile.bio) }
        var avatar by remember(profile) { mutableStateOf(profile.avatarUrl) }
        var favorites by remember(profile) { mutableStateOf(profile.favoriteTitles) }
        var banner by remember(profile) { mutableIntStateOf(profile.bannerStyle) }
        var accent by remember(profile) { mutableIntStateOf(profile.accentStyle) }
        AlertDialog(
            onDismissRequest = { editing = false }, containerColor = Panel2,
            title = { Text("Customize your profile", color = White, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Display name", color = Muted, fontSize = 11.sp)
                    OutlinedTextField(name, { name = it.take(32) }, singleLine = true,
                        placeholder = { Text("Your display name") })
                    Text("Bio", color = Muted, fontSize = 11.sp)
                    OutlinedTextField(bio, { bio = it.take(140) }, minLines = 2,
                        placeholder = { Text("A little about your anime taste") })
                    Text("Avatar image URL (optional)", color = Muted, fontSize = 11.sp)
                    OutlinedTextField(avatar, { avatar = it.take(500) }, singleLine = true,
                        placeholder = { Text("https://…") })
                    Text("Favorite titles", color = Muted, fontSize = 11.sp)
                    OutlinedTextField(favorites, { favorites = it.take(180) }, minLines = 2,
                        placeholder = { Text("Separate titles with commas") })
                    Text("Accent color", color = Muted, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccentPalette.forEachIndexed { index, color ->
                            Box(Modifier.weight(1f).height(34.dp)
                                .background(color, RoundedCornerShape(10.dp))
                                .clickable { accent = index }) {
                                if (accent == index) Icon(Icons.Default.Check, null, tint = Ink,
                                    modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    }
                    Text("Profile banner", color = Muted, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bannerColors.forEachIndexed { index, colors ->
                            Box(Modifier.weight(1f).height(38.dp)
                                .background(Brush.linearGradient(colors), RoundedCornerShape(9.dp))
                                .clickable { banner = index }) {
                                if (banner == index) Icon(Icons.Default.Check, null, tint = White,
                                    modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    profile = ProfileSettings(name.ifBlank { "Anime Explorer" }, bio, avatar, banner, favorites, accent)
                    VioletState.value = AccentPalette[accent.coerceIn(0, AccentPalette.lastIndex)]
                    saveProfile(context, profile)
                    editing = false
                }) { Text("Save profile", color = Violet) }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel", color = Muted) } }
        )
    }
}

@Composable
private fun ProfileStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(14.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Muted, fontSize = 10.sp)
    }
}

internal data class LocalTrack(val uri: String, val title: String)

private fun <T> JSONArray.mapSafe(block: (JSONObject) -> T): List<T> =
    (0 until length()).mapNotNull { index -> runCatching { block(getJSONObject(index)) }.getOrNull() }

private fun JSONArray?.toStrings(): List<String> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }

private fun readJsonArray(context: Context, key: String): JSONArray? {
    val prefs = context.getSharedPreferences("anilarper", Context.MODE_PRIVATE)
    val raw = prefs.getString(key, "[]") ?: "[]"
    return runCatching { JSONArray(raw) }.getOrElse {
        prefs.edit().putString("${key}_corrupt_backup", raw).apply()
        null
    }
}

internal fun loadTracks(context: Context): List<LocalTrack> =
    readJsonArray(context, "tracks")?.mapSafe { item ->
        LocalTrack(item.getString("uri"), item.optString("title", "Audio track").ifBlank { "Audio track" })
    } ?: emptyList()

internal fun persistTracks(context: Context, tracks: List<LocalTrack>) {
    val array = JSONArray()
    tracks.forEach { array.put(JSONObject().put("uri", it.uri).put("title", it.title)) }
    context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).edit().putString("tracks", array.toString()).apply()
}

private fun loadReadingLibrary(context: Context): List<ReadingTitle> =
    app.anilarper.personal.data.LibraryRepository(context).loadReading()

private fun persistReadingLibrary(context: Context, library: List<ReadingTitle>) {
    app.anilarper.personal.data.LibraryRepository(context).saveReading(library)
}

@Composable
private fun ModuleScreen(title: String, subtitle: String, items: List<String>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Spacer(Modifier.height(12.dp))
        Text(title, color = White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 14.sp)
        items.forEach { item ->
            Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Violet)
                Spacer(Modifier.width(12.dp))
                Text(item, color = White, fontSize = 13.sp)
            }
        }
        Text("More features are being built in stages.", color = Violet, fontSize = 12.sp)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

@Composable
internal fun QuickTile(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.background(Panel, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = Violet, modifier = Modifier.size(22.dp))
        Text(title, color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(subtitle, color = Muted, fontSize = 10.sp, lineHeight = 13.sp)
    }
}

@Composable
private fun FeatureLine(text: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Violet, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = White, fontSize = 13.sp)
    }
}

private fun loadLibrary(context: Context): List<Anime> =
    app.anilarper.personal.data.LibraryRepository(context).loadAnime()

private fun persistLibrary(context: Context, library: List<Anime>) {
    app.anilarper.personal.data.LibraryRepository(context).saveAnime(library)
}
