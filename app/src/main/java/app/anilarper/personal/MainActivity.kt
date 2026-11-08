package app.anilarper.personal

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
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
    var moreOpen by remember { mutableStateOf(false) }
    var library by remember { mutableStateOf(loadLibrary(context)) }
    var readingLibrary by remember { mutableStateOf(loadReadingLibrary(context)) }
    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Panel, primary = Violet,
        onBackground = White, onSurface = White
    )) {
        Scaffold(
            containerColor = Ink,
            bottomBar = {
                NavigationBar(containerColor = Panel, tonalElevation = 8.dp) {
                    listOf("Home" to Icons.Default.Home, "Discover" to Icons.Default.Explore,
                        "Library" to Icons.Default.Bookmarks, "More" to Icons.Default.MoreHoriz).forEach { (label, icon) ->
                        NavigationBarItem(
                            selected = if (label == "More") selected in setOf("Read", "Music", "Quests", "Profile") else selected == label,
                            onClick = { if (label == "More") moreOpen = true else selected = label },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Violet, selectedTextColor = Violet,
                                indicatorColor = Violet.copy(alpha = .16f),
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
                "Read" -> ReadingScreen(Modifier.padding(padding), readingLibrary,
                    onAdd = { title ->
                        if (readingLibrary.none { it.id == title.id && it.kind == title.kind }) {
                            readingLibrary = readingLibrary + title
                            persistReadingLibrary(context, readingLibrary)
                        }
                    },
                    onUpdate = { updated ->
                        readingLibrary = readingLibrary.map { if (it.id == updated.id && it.kind == updated.kind) updated else it }
                        persistReadingLibrary(context, readingLibrary)
                    },
                    onRemove = { title ->
                        readingLibrary = readingLibrary.filterNot { it.id == title.id && it.kind == title.kind }
                        persistReadingLibrary(context, readingLibrary)
                    }
                )
                "Music" -> MusicScreen(Modifier.padding(padding))
                "Quests" -> QuestScreen(context, library, readingLibrary, Modifier.padding(padding))
                else -> ProfileScreen(context, library, readingLibrary, Modifier.padding(padding))
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
                        Triple("Profile", "Your personal space", Icons.Default.Person)
                    ).forEach { (label, subtitle, icon) ->
                        Row(Modifier.fillMaxWidth().clickable { selected = label; moreOpen = false }
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
private fun ReadingScreen(
    modifier: Modifier,
    library: List<ReadingTitle>,
    onAdd: (ReadingTitle) -> Unit,
    onUpdate: (ReadingTitle) -> Unit,
    onRemove: (ReadingTitle) -> Unit
) {
    var novels by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ReadingTitle>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    fun search() {
        scope.launch {
            loading = true
            error = null
            try { results = AniListReadingApi.search(query, novels) }
            catch (_: Exception) { error = "Catalog unavailable. Check your connection and retry." }
            finally { loading = false }
        }
    }
    LaunchedEffect(novels) { search() }
    val visibleSaved = library.filter { it.kind == if (novels) "NOVEL" else "MANGA" }
    Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(18.dp))
        Text("YOUR READING SHELF", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text(if (novels) "Light novels." else "Manga.", color = White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Track chapters and volumes at your own pace.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !novels, onClick = { novels = false }, label = { Text("Manga") })
            FilterChip(selected = novels, onClick = { novels = true }, label = { Text("Light novels") })
        }
        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(if (novels) "Search light novels…" else "Search manga…", color = Muted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Muted) },
            singleLine = true, shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); search() }),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Violet, unfocusedBorderColor = Panel2,
                focusedTextColor = White, unfocusedTextColor = White, cursorColor = Violet))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("YOUR SAVED TITLES", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { keyboard?.hide(); search() }) { Text("Search", color = Violet) }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 20.dp),
            modifier = Modifier.weight(1f)) {
            items(visibleSaved, key = { it.kind + "-" + it.id }) { title ->
                Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = title.cover, contentDescription = title.title,
                            modifier = Modifier.width(54.dp).height(72.dp).background(Panel2, RoundedCornerShape(8.dp)))
                        Column(Modifier.weight(1f)) {
                            Text(title.title, color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2)
                            Text(title.format + " • " + title.readingStatus, color = Violet, fontSize = 10.sp)
                            Text(title.progress.toString() + (title.chapters?.let { " / " + it } ?: "") + " chapters",
                                color = Muted, fontSize = 10.sp)
                            Text(title.volumeProgress.toString() + (title.volumes?.let { " / " + it } ?: "") + " volumes",
                                color = Muted, fontSize = 10.sp)
                        }
                        IconButton(onClick = { onRemove(title) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove title", tint = Muted)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Chapters", color = Muted, fontSize = 11.sp)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { onUpdate(title.copy(progress = (title.progress - 1).coerceAtLeast(0))) },
                            enabled = title.progress > 0) { Icon(Icons.Default.RemoveCircleOutline, null, tint = Violet) }
                        Text(title.progress.toString(), color = White, fontWeight = FontWeight.Bold)
                        IconButton(onClick = {
                            val next = title.progress + 1
                            onUpdate(title.copy(progress = title.chapters?.let { next.coerceAtMost(it) } ?: next,
                                readingStatus = if (title.chapters != null && next >= title.chapters) "COMPLETED" else "READING"))
                        }, enabled = title.chapters == null || title.progress < title.chapters) {
                            Icon(Icons.Default.AddCircleOutline, null, tint = Violet)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Volumes", color = Muted, fontSize = 11.sp)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { onUpdate(title.copy(volumeProgress = (title.volumeProgress - 1).coerceAtLeast(0))) },
                            enabled = title.volumeProgress > 0) { Icon(Icons.Default.Remove, null, tint = Violet) }
                        Text(title.volumeProgress.toString(), color = White, fontWeight = FontWeight.Bold)
                        IconButton(onClick = {
                            val next = title.volumeProgress + 1
                            onUpdate(title.copy(volumeProgress = title.volumes?.let { next.coerceAtMost(it) } ?: next))
                        }, enabled = title.volumes == null || title.volumeProgress < title.volumes) {
                            Icon(Icons.Default.Add, null, tint = Violet)
                        }
                    }
                }
            }
            item { Text("DISCOVER", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
            if (loading) item { Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Violet)
            } }
            error?.let { message -> item { Text(message, color = Muted, fontSize = 12.sp) } }
            items(results, key = { "result-" + it.kind + "-" + it.id }) { title ->
                val saved = library.any { it.id == title.id && it.kind == title.kind }
                Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(13.dp)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AsyncImage(model = title.cover, contentDescription = title.title,
                        modifier = Modifier.width(52.dp).height(70.dp).background(Panel2, RoundedCornerShape(8.dp)))
                    Column(Modifier.weight(1f)) {
                        Text(title.title, color = White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 2)
                        Text(title.format + " • " + (title.chapters?.let { it.toString() + " chapters" } ?: "Chapter count unknown"),
                            color = Muted, fontSize = 10.sp)
                        Text(title.volumes?.let { it.toString() + " volumes" } ?: "Volumes unknown", color = Muted, fontSize = 10.sp)
                    }
                    IconButton(onClick = { if (!saved) onAdd(title) }, enabled = !saved) {
                        Icon(if (saved) Icons.Default.CheckCircle else Icons.Default.AddCircle,
                            contentDescription = if (saved) "Saved" else "Add to reading shelf",
                            tint = if (saved) Violet else White)
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var tracks by remember { mutableStateOf(loadTracks(context)) }
    var current by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkTitle by remember { mutableStateOf("") }
    var linkUrl by remember { mutableStateOf("") }
    var links by remember { mutableStateOf(loadMusicLinks(context)) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val track = LocalTrack(uri.toString(), uri.lastPathSegment?.substringAfterLast('/')?.substringBefore('?') ?: "Audio track")
            if (tracks.none { it.uri == track.uri }) { tracks = tracks + track; persistTracks(context, tracks) }
            message = "Added to your local soundtrack library."
        }
    }
    DisposableEffect(player) { onDispose { player?.release() } }
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
                            if (playing) { player?.pause(); playing = false } else { player?.start(); playing = true }
                        } else {
                            runCatching {
                                player?.release()
                                val next = MediaPlayer.create(context, Uri.parse(track.uri)) ?: error("Audio file couldn't be opened.")
                                next.setOnCompletionListener { playing = false }
                                player = next; current = track.uri; next.start(); playing = true; message = null
                            }.onFailure { message = it.message ?: "Unable to play this file." }
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
                    Text("NOW PLAYING · LOCAL", color = Violet, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(tracks.firstOrNull { it.uri == current }?.title ?: "Audio track", color = White, fontSize = 12.sp, maxLines = 1)
                }
                IconButton(onClick = { if (playing) { player?.pause(); playing = false } else { player?.start(); playing = true } }) {
                    Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play or pause", tint = Violet)
                }
                IconButton(onClick = { player?.seekTo(0); if (!playing) { player?.start(); playing = true } }) {
                    Icon(Icons.Default.Replay, contentDescription = "Restart track", tint = Violet)
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

private data class MusicLink(val title: String, val url: String, val source: String)

private fun loadMusicLinks(context: Context): List<MusicLink> = try {
    val a = JSONArray(context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).getString("musicLinks", "[]") ?: "[]")
    (0 until a.length()).map { i ->
        val o = a.getJSONObject(i)
        MusicLink(o.getString("title"), o.getString("url"), o.getString("source"))
    }
} catch (_: Exception) { emptyList() }

private fun persistMusicLinks(context: Context, links: List<MusicLink>) {
    val a = JSONArray()
    links.forEach { a.put(JSONObject().put("title", it.title).put("url", it.url).put("source", it.source)) }
    context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).edit().putString("musicLinks", a.toString()).apply()
}

private data class LocalTrack(val uri: String, val title: String)

private fun loadTracks(context: Context): List<LocalTrack> = try {
    val array = JSONArray(context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).getString("tracks", "[]") ?: "[]")
    (0 until array.length()).map { i ->
        val item = array.getJSONObject(i)
        LocalTrack(item.getString("uri"), item.getString("title"))
    }
} catch (_: Exception) { emptyList() }

private fun persistTracks(context: Context, tracks: List<LocalTrack>) {
    val array = JSONArray()
    tracks.forEach { array.put(JSONObject().put("uri", it.uri).put("title", it.title)) }
    context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).edit().putString("tracks", array.toString()).apply()
}

private fun loadReadingLibrary(context: Context): List<ReadingTitle> = try {
    val array = JSONArray(context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).getString("readingLibrary", "[]") ?: "[]")
    (0 until array.length()).map { i ->
        val o = array.getJSONObject(i)
        ReadingTitle(o.getInt("id"), o.getString("title"), o.optString("nativeTitle"), o.optString("cover"),
            o.optString("format"), o.optString("status"), if (o.isNull("chapters")) null else o.optInt("chapters"),
            if (o.isNull("volumes")) null else o.optInt("volumes"), o.optString("description"),
            o.optString("kind", "MANGA"), o.optInt("progress"), o.optInt("volumeProgress"), o.optString("readingStatus", "PLANNING"))
    }
} catch (_: Exception) { emptyList() }

private fun persistReadingLibrary(context: Context, library: List<ReadingTitle>) {
    val array = JSONArray()
    library.forEach { item ->
        array.put(JSONObject().put("id", item.id).put("title", item.title).put("nativeTitle", item.nativeTitle)
            .put("cover", item.cover).put("format", item.format).put("status", item.status)
            .put("chapters", item.chapters).put("volumes", item.volumes).put("description", item.description)
            .put("kind", item.kind).put("progress", item.progress).put("volumeProgress", item.volumeProgress)
            .put("readingStatus", item.readingStatus))
    }
    context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).edit().putString("readingLibrary", array.toString()).apply()
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
