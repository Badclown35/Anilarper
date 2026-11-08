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
import app.anilarper.personal.ui.backup.BackupScreen
import app.anilarper.personal.ui.stats.WatchHistoryScreen
import app.anilarper.personal.ui.stats.StatisticsScreen
import app.anilarper.personal.ui.profile.ProfileScreen
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
