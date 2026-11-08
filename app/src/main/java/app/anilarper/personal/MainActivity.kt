package app.anilarper.personal

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Ink = Color(0xFF0C0D12)
private val Panel = Color(0xFF171923)
private val Panel2 = Color(0xFF202331)
private val Violet = Color(0xFF9B8CFF)
private val Muted = Color(0xFF9A9EAE)
private val White = Color(0xFFF4F3FA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AnilarperApp() }
    }
}

@Composable
private fun AnilarperApp() {
    val context = LocalContext.current
    var selected by remember { mutableStateOf("Home") }
    var library by remember { mutableStateOf(loadLibrary(context)) }
    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Panel, primary = Violet,
        onBackground = White, onSurface = White
    )) {
        Scaffold(
            containerColor = Ink,
            bottomBar = {
                NavigationBar(containerColor = Panel) {
                    listOf("Home" to Icons.Default.Home, "Discover" to Icons.Default.Explore,
                        "Library" to Icons.Default.Bookmarks, "Quests" to Icons.Default.EmojiEvents,
                        "Profile" to Icons.Default.Person).forEach { (label, icon) ->
                        NavigationBarItem(
                            selected = selected == label, onClick = { selected = label },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Violet, selectedTextColor = Violet,
                                indicatorColor = Violet.copy(alpha = .14f),
                                unselectedIconColor = Muted, unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            when (selected) {
                "Home" -> HomeScreen(Modifier.padding(padding)) { selected = "Discover" }
                "Discover" -> DiscoverScreen(Modifier.padding(padding), library.map { it.id }.toSet()) { anime ->
                    library = if (library.any { it.id == anime.id }) library.filterNot { it.id == anime.id }
                    else library + anime
                    persistLibrary(context, library)
                }
                "Library" -> LibraryScreen(Modifier.padding(padding), library,
                    onRemove = { anime ->
                        library = library.filterNot { it.id == anime.id }
                        persistLibrary(context, library)
                    },
                    onUpdate = { updated ->
                        library = library.map { if (it.id == updated.id) updated else it }
                        persistLibrary(context, library)
                    }
                )
                "Quests" -> ModuleScreen("Nexus quests", "Small goals, on your schedule.",
                    listOf("Explore one new series", "Update an episode count", "Add a manga to your list"), Modifier.padding(padding))
                else -> ModuleScreen("Your profile", "Your anime identity, stats, and collection.",
                    listOf("Your saved titles", "Personal stats are coming next"), Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun HomeScreen(modifier: Modifier = Modifier, onDiscover: () -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ANILARPER", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Your next chapter.", color = White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.size(42.dp).background(Panel2, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = "Profile", tint = Violet)
            }
        }
        Box(Modifier.fillMaxWidth().background(
            Brush.linearGradient(listOf(Color(0xFF30294F), Color(0xFF171923), Color(0xFF252039))),
            RoundedCornerShape(24.dp)).padding(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("YOUR COMEBACK STARTS HERE", color = Violet, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Text("Find the feeling\nthat got you into anime.", color = White, fontSize = 24.sp,
                    fontWeight = FontWeight.Bold, lineHeight = 29.sp)
                Text("A personal space for anime, manga, and discoveries.", color = Muted, fontSize = 13.sp)
                Button(onClick = onDiscover, colors = ButtonDefaults.buttonColors(
                    containerColor = Violet, contentColor = Ink), shape = RoundedCornerShape(14.dp)) {
                    Text("Explore anime", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
        SectionTitle("Your space", "Everything in one place")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTile("Anime", "Find series", Icons.Default.Tv, Modifier.weight(1f), onDiscover)
            QuickTile("Manga", "Coming next", Icons.Default.MenuBook, Modifier.weight(1f), onDiscover)
            QuickTile("Quests", "At your pace", Icons.Default.EmojiEvents, Modifier.weight(1f), onDiscover)
        }
        SectionTitle("A little inspiration", "No pressure. Just discovery.")
        Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("START WITH WHAT YOU LIKE", color = Violet, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text("Your next favorite is out there.", color = White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("Browse trending anime, search for a specific title, and save anything that catches your interest.",
                color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
            TextButton(onClick = onDiscover) { Text("Open discovery  →", color = Violet) }
        }
        SectionTitle("Building Anilarper", "The first working modules")
        Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).padding(16.dp)) {
            FeatureLine("Live anime catalog and search")
            FeatureLine("Personal saved library")
            FeatureLine("Manga tracking and recommendations")
            FeatureLine("XP, quests, and achievements")
            FeatureLine("MAL list import")
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun DiscoverScreen(modifier: Modifier = Modifier, savedIds: Set<Int>, onToggleSaved: (Anime) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Anime>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedAnime by remember { mutableStateOf<Anime?>(null) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    fun search(term: String) {
        scope.launch {
            loading = true
            error = null
            try { results = AniListApi.search(term) }
            catch (_: Exception) { error = "Couldn't reach AniList. Check your connection and try again." }
            finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { search("") }

    Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(18.dp))
        Text("DISCOVER", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Find your next watch.", color = White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Search the catalog or explore what's trending.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search anime titles…", color = Muted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Muted) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = ""; search("") }) {
                Icon(Icons.Default.Close, contentDescription = "Clear")
            } },
            singleLine = true, shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); search(query) }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Violet, unfocusedBorderColor = Panel2,
                focusedTextColor = White, unfocusedTextColor = White, cursorColor = Violet
            )
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (query.isBlank()) "TRENDING NOW" else "SEARCH RESULTS", color = Violet,
                fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { keyboard?.hide(); search(query) }) { Text("Search", color = Violet) }
        }
        when {
            loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Violet)
            }
            error != null -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(error!!, color = Muted)
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 18.dp), modifier = Modifier.weight(1f)) {
                items(results, key = { it.id }) { anime ->
                    AnimeRow(anime, savedIds.contains(anime.id)) { selectedAnime = anime }
                }
            }
        }
    }
    selectedAnime?.let { anime ->
        AlertDialog(
            onDismissRequest = { selectedAnime = null },
            containerColor = Panel2,
            title = { Text(anime.title, color = White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AsyncImage(model = anime.cover, contentDescription = anime.title,
                        modifier = Modifier.fillMaxWidth().height(170.dp))
                    Text("${anime.format} • ${anime.status}", color = Violet, fontSize = 12.sp)
                    Text("${anime.episodes?.let { "$it episodes" } ?: "Episode count unknown"} • ${anime.score?.let { "$it% score" } ?: "Not rated"}",
                        color = Muted, fontSize = 11.sp)
                    Text(anime.description.ifBlank { "No synopsis available." }, color = Muted, fontSize = 12.sp, maxLines = 6)
                }
            },
            confirmButton = {
                TextButton(onClick = { onToggleSaved(anime) }) {
                    Text(if (savedIds.contains(anime.id)) "Remove from library" else "Add to library", color = Violet)
                }
            },
            dismissButton = { TextButton(onClick = { selectedAnime = null }) { Text("Close", color = Muted) } }
        )
    }
}

@Composable
private fun AnimeRow(anime: Anime, saved: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp))
        .clickable(onClick = onClick).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AsyncImage(model = anime.cover, contentDescription = anime.title,
            modifier = Modifier.width(76.dp).height(104.dp).background(Panel2, RoundedCornerShape(10.dp)))
        Column(Modifier.weight(1f).height(104.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(anime.title, color = White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2)
                Text(listOfNotNull(anime.format.takeIf { it != "UNKNOWN" }, anime.episodes?.let { "$it eps" }).joinToString(" • "),
                    color = Muted, fontSize = 11.sp)
                anime.score?.let { Text("★ $it%", color = Violet, fontSize = 11.sp) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (saved) "IN YOUR LIBRARY" else "VIEW DETAILS", color = Violet,
                    fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Muted)
            }
        }
    }
}

@Composable
private fun LibraryScreen(modifier: Modifier = Modifier, library: List<Anime>, onRemove: (Anime) -> Unit, onUpdate: (Anime) -> Unit) {
    Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(20.dp))
        Text("YOUR LIBRARY", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Saved for later.", color = White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("${library.size} saved anime • ${library.count { it.watchStatus == "COMPLETED" }} completed", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        if (library.isEmpty()) {
            Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Bookmarks, contentDescription = null, tint = Violet, modifier = Modifier.size(32.dp))
                Text("Your list starts here.", color = White, fontWeight = FontWeight.SemiBold)
                Text("Find anime in Discover and save titles you want to keep track of.", color = Muted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
                items(library, key = { it.id }) { anime ->
                    Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AsyncImage(model = anime.cover, contentDescription = anime.title,
                                modifier = Modifier.width(58.dp).height(76.dp).background(Panel2, RoundedCornerShape(8.dp)))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(anime.title, color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2)
                                Text(anime.watchStatus.replace('_', ' '), color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(if (anime.episodes != null) "${anime.progress} / ${anime.episodes} episodes" else "${anime.progress} episodes tracked",
                                    color = Muted, fontSize = 11.sp)
                            }
                            IconButton(onClick = { onRemove(anime) }) {
                                Icon(Icons.Default.BookmarkRemove, contentDescription = "Remove", tint = Muted)
                            }
                        }
                        if (anime.episodes != null && anime.episodes > 0) {
                            LinearProgressIndicator(
                                progress = { (anime.progress.toFloat() / anime.episodes).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(5.dp),
                                color = Violet, trackColor = Panel2
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Episodes watched", color = Muted, fontSize = 11.sp)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { onUpdate(anime.copy(progress = (anime.progress - 1).coerceAtLeast(0))) },
                                enabled = anime.progress > 0) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease episode", tint = Violet)
                            }
                            Text(anime.progress.toString(), color = White, fontWeight = FontWeight.Bold)
                            IconButton(onClick = {
                                val next = anime.progress + 1
                                val capped = anime.episodes?.let { next.coerceAtMost(it) } ?: next
                                onUpdate(anime.copy(progress = capped,
                                    watchStatus = if (anime.episodes != null && capped >= anime.episodes) "COMPLETED" else "WATCHING"))
                            }, enabled = anime.episodes == null || anime.progress < anime.episodes) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase episode", tint = Violet)
                            }
                        }
                        var statusExpanded by remember(anime.id) { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { statusExpanded = true }) {
                                Text("Change status", color = Violet, fontSize = 11.sp)
                                Icon(Icons.Default.ExpandMore, contentDescription = null, tint = Violet)
                            }
                            DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                                listOf("PLANNING", "WATCHING", "COMPLETED", "PAUSED", "DROPPED").forEach { status ->
                                    DropdownMenuItem(text = { Text(status.replace('_', ' ')) },
                                        onClick = {
                                            statusExpanded = false
                                            onUpdate(anime.copy(watchStatus = status))
                                        })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
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
private fun QuickTile(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
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

private fun loadLibrary(context: Context): List<Anime> = try {
    val raw = context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).getString("library", "[]") ?: "[]"
    val array = JSONArray(raw)
    (0 until array.length()).map { i ->
        val o = array.getJSONObject(i)
        Anime(o.getInt("id"), o.getString("title"), o.optString("nativeTitle"), o.optString("cover"),
            o.optString("format"), o.optString("status"),
            if (o.isNull("episodes")) null else o.optInt("episodes"),
            if (o.isNull("score")) null else o.optInt("score"), o.optString("description"),
            o.optString("watchStatus", "PLANNING"), o.optInt("progress", 0))
    }
} catch (_: Exception) { emptyList() }

private fun persistLibrary(context: Context, library: List<Anime>) {
    val array = JSONArray()
    library.forEach { a ->
        array.put(JSONObject().put("id", a.id).put("title", a.title).put("nativeTitle", a.nativeTitle)
            .put("cover", a.cover).put("format", a.format).put("status", a.status)
            .put("episodes", a.episodes).put("score", a.score).put("description", a.description)
            .put("watchStatus", a.watchStatus).put("progress", a.progress))
    }
    context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).edit()
        .putString("library", array.toString()).apply()
}
