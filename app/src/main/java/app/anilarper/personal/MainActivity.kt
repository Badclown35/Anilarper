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
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.lifecycle.ViewModelProvider
import app.anilarper.personal.data.AnimeLibraryViewModel
import app.anilarper.personal.data.ReadingLibraryViewModel
import app.anilarper.personal.data.CollectionViewModelFactory
import coil.compose.AsyncImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

internal val Ink = Color(0xFF0C0D12)
internal val Panel = Color(0xFF171923)
internal val Panel2 = Color(0xFF202331)
private val AccentPalette = listOf(
    Color(0xFF9B8CFF), Color(0xFF72C8D8), Color(0xFFFF9FBD), Color(0xFFFFC36E), Color(0xFF91CFA5)
)
internal val VioletState = mutableStateOf(AccentPalette.first())
internal val Violet: Color get() = VioletState.value
internal val Muted = Color(0xFF9A9EAE)
internal val White = Color(0xFFF4F3FA)

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
    var selected by rememberSaveable { mutableStateOf("Home") }
    var moreOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = selected != "Home" || moreOpen) {
        if (moreOpen) moreOpen = false else selected = "Home"
    }
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
    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Panel, primary = Violet,
        onBackground = White, onSurface = White
    )) {
        Scaffold(
            containerColor = Ink,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF11131B), tonalElevation = 0.dp) {
                    listOf("Home" to Icons.Default.Home, "Discover" to Icons.Default.Explore,
                        "Library" to Icons.Default.Bookmarks, "More" to Icons.Default.MoreHoriz).forEach { (label, icon) ->
                        NavigationBarItem(
                            selected = if (label == "More") selected in setOf("Read", "Music", "Quests", "Profile", "Stats", "Backup", "History") else selected == label,
                            onClick = { if (label == "More") moreOpen = true else selected = label },
                            icon = { Icon(icon, contentDescription = label,
                                modifier = Modifier.size(if (selected == label) 23.dp else 21.dp)) },
                            label = { Text(label, fontSize = 10.sp,
                                fontWeight = if (selected == label) FontWeight.Bold else FontWeight.Medium, maxLines = 1) },
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
            when (selected) {
                "Home" -> HomeScreen(
                    Modifier.padding(padding), library,
                    onDiscover = { selected = "Discover" },
                    onOpenLibrary = { selected = "Library" },
                    onOpenRead = { selected = "Read" },
                    onOpenQuests = { selected = "Quests" },
                    onOpenStats = { selected = "Stats" },
                    onOpenProfile = { selected = "Profile" }
                )
                "Discover" -> DiscoverScreen(Modifier.padding(padding), library.map { it.id }.toSet()) { anime ->
                    animeViewModel.toggle(anime)
                }
                "Library" -> LibraryScreen(Modifier.padding(padding), library,
                    onRemove = { anime ->
                        animeViewModel.remove(anime)
                    },
                    onUpdate = { updated ->
                        animeViewModel.update(updated)
                    }
                )
                "Read" -> ReadingScreen(Modifier.padding(padding), readingLibrary,
                    onAdd = { title ->
                        readingViewModel.add(title)
                    },
                    onUpdate = { updated ->
                        readingViewModel.update(updated)
                    },
                    onRemove = { title ->
                        readingViewModel.remove(title)
                    }
                )
                "Music" -> MusicScreen(Modifier.padding(padding))
                "Quests" -> QuestScreen(context, library, readingLibrary, Modifier.padding(padding))
                "Stats" -> StatisticsScreen(library, readingLibrary, Modifier.padding(padding))
                "History" -> WatchHistoryScreen(library, Modifier.padding(padding))
                "Backup" -> BackupScreen(context, Modifier.padding(padding), onRestored = {
                    animeViewModel.refresh()
                    readingViewModel.refresh()
                    val accent = context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).getInt("profileAccent", 0)
                    VioletState.value = AccentPalette[accent.coerceIn(0, AccentPalette.lastIndex)]
                })
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
                        Triple("Stats", "Your anime and reading activity", Icons.Default.Insights),
                        Triple("History", "Recently updated episodes", Icons.Default.History),
                        Triple("Backup", "Export or restore your collection", Icons.Default.Backup),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverScreen(modifier: Modifier = Modifier, savedIds: Set<Int>, onToggleSaved: (Anime) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Anime>>(emptyList()) }
    var page by remember { mutableIntStateOf(1) }
    var hasNext by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedAnime by remember { mutableStateOf<Anime?>(null) }
    var detail by remember { mutableStateOf<Anime?>(null) }
    var detailLoading by remember { mutableStateOf(false) }
    var detailError by remember { mutableStateOf<String?>(null) }
    var franchise by remember { mutableStateOf<FranchiseResult?>(null) }
    var franchiseLoading by remember { mutableStateOf(false) }
    var franchiseError by remember { mutableStateOf<String?>(null) }
    var franchiseRetry by remember { mutableIntStateOf(0) }
    var franchiseSortByYear by remember { mutableStateOf(false) }
    var genre by remember { mutableStateOf<String?>(null) }
    var format by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var season by remember { mutableStateOf<String?>(null) }
    var seasonYear by remember { mutableIntStateOf(2026) }
    var sort by remember { mutableStateOf("TRENDING_DESC") }
    var filterMenu by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var loadJob by remember { mutableStateOf<Job?>(null) }

    fun load(reset: Boolean = true) {
        loadJob?.cancel()
        loadJob = scope.launch {
            if (reset) { loading = true; page = 1; hasNext = false } else loadingMore = true
            error = null
            try {
                val nextPage = if (reset) 1 else page + 1
                val fetched = AniListApi.search(query, nextPage,
                    AnimeFilters(genre, format, status, season, if (season == null) null else seasonYear, sort))
                results = if (reset) fetched.items else (results + fetched.items).distinctBy { it.id }
                page = fetched.page
                hasNext = fetched.hasNextPage
            } catch (e: Exception) {
                error = if (e.message?.contains("rate", true) == true)
                    "AniList is receiving too many requests. Wait a moment, then retry."
                else "Couldn't load anime. Check your connection and try again."
            } finally { if (isActive) { loading = false; loadingMore = false } }
        }
    }
    LaunchedEffect(Unit) { load() }
    LaunchedEffect(selectedAnime?.id, franchiseRetry) {
        val selected = selectedAnime ?: return@LaunchedEffect
        detail = selected
        detailError = null
        franchise = null
        franchiseError = null
        detailLoading = true
        franchiseLoading = true

        // Details and franchise discovery are independent requests. Either can
        // succeed even if the other one fails.
        try {
            detail = AniListApi.details(selected.id)
        } catch (e: Exception) {
            detailError = "Extra details unavailable: ${e.message?.take(100) ?: "network or AniList error"}"
        } finally {
            detailLoading = false
        }

        try {
            franchise = AniListApi.franchise(selected.id, selected)
        } catch (e: Exception) {
            franchiseError = "Franchise scan failed: ${e.message?.take(100) ?: "network or AniList error"}"
        } finally {
            franchiseLoading = false
        }
    }

    Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("DISCOVER", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Find your next watch.", color = White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Browse seasons, filter the catalog, and follow connected titles.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search anime titles…", color = Muted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Muted) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = ""; keyboard?.hide(); load() }) {
                Icon(Icons.Default.Close, contentDescription = "Clear")
            } },
            singleLine = true, shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); load() }),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Violet,
                unfocusedBorderColor = Panel2, focusedTextColor = White,
                unfocusedTextColor = White, cursorColor = Violet))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 7.dp)) {
            val filters = listOf(
                Triple("Genre", genre ?: "Genre", listOf("Action","Adventure","Comedy","Drama","Fantasy","Horror","Mystery","Romance","Sci-Fi","Slice of Life","Sports","Supernatural","Thriller")),
                Triple("Format", format?.replace('_',' ') ?: "Format", listOf("TV","TV_SHORT","MOVIE","SPECIAL","OVA","ONA")),
                Triple("Status", status?.replace('_',' ') ?: "Status", listOf("RELEASING","FINISHED","NOT_YET_RELEASED","CANCELLED","HIATUS")),
                Triple("Season", season ?: "Season", listOf("WINTER","SPRING","SUMMER","FALL")),
                Triple("Sort", when(sort) { "SCORE_DESC" -> "Top score"; "POPULARITY_DESC" -> "Popular"; "START_DATE_DESC" -> "Newest"; else -> "Trending" },
                    listOf("TRENDING_DESC","POPULARITY_DESC","SCORE_DESC","START_DATE_DESC"))
            )
            filters.forEach { (key, label, options) ->
                Box {
                    FilterChip(selected = (key == "Genre" && genre != null) || (key == "Format" && format != null) ||
                        (key == "Status" && status != null) || (key == "Season" && season != null) ||
                        (key == "Sort" && sort != "TRENDING_DESC"),
                        onClick = { filterMenu = if (filterMenu == key) "" else key },
                        label = { Text(label, maxLines = 1, fontSize = 10.sp) })
                    DropdownMenu(expanded = filterMenu == key, onDismissRequest = { filterMenu = "" }) {
                        if (key != "Sort") DropdownMenuItem(text = { Text("Any " + key.lowercase()) }, onClick = {
                            when(key) { "Genre" -> genre = null; "Format" -> format = null; "Status" -> status = null; "Season" -> season = null }
                            filterMenu = ""; load()
                        })
                        options.forEach { option -> DropdownMenuItem(text = { Text(option.replace('_',' ')) }, onClick = {
                            when(key) {
                                "Genre" -> genre = option
                                "Format" -> format = option
                                "Status" -> status = option
                                "Season" -> season = option
                                "Sort" -> sort = option
                            }
                            filterMenu = ""; load()
                        }) }
                    }
                }
            }
        }
        if (season != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Year", color = Muted, fontSize = 11.sp)
                TextButton(onClick = { seasonYear = (seasonYear - 1).coerceAtLeast(1970); load() }) { Text("−", color = Violet) }
                Text(seasonYear.toString(), color = White, fontWeight = FontWeight.Bold)
                TextButton(onClick = { seasonYear = (seasonYear + 1).coerceAtMost(2035); load() }) { Text("+", color = Violet) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (query.isBlank()) "CATALOG" else "SEARCH RESULTS", color = Violet,
                fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { keyboard?.hide(); load() }) { Text("Apply", color = Violet) }
        }
        when {
            loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Violet)
            }
            error != null && results.isEmpty() -> Column(Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error!!, color = Muted)
                TextButton(onClick = { load() }) { Text("Retry", color = Violet) }
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp),
                contentPadding = PaddingValues(bottom = 20.dp), modifier = Modifier.weight(1f)) {
                items(results, key = { it.id }) { anime ->
                    AnimeRow(anime, savedIds.contains(anime.id)) { selectedAnime = anime }
                }
                if (error != null) item { Text(error!!, color = Muted, fontSize = 11.sp) }
                if (hasNext) item {
                    Button(onClick = { load(false) }, enabled = !loadingMore, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Panel2, contentColor = Violet)) {
                        if (loadingMore) CircularProgressIndicator(modifier = Modifier.size(17.dp), color = Violet)
                        else Text("Load more anime")
                    }
                }
            }
        }
    }
    selectedAnime?.let { anime ->
        val shown = detail ?: anime
        AlertDialog(onDismissRequest = { selectedAnime = null }, containerColor = Panel2,
            title = { Text(shown.title, color = White, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AsyncImage(model = shown.cover, contentDescription = shown.title,
                        modifier = Modifier.fillMaxWidth().height(180.dp))
                    Text(listOfNotNull(shown.format, shown.status,
                        shown.season.takeIf { it.isNotBlank() }?.lowercase()?.replaceFirstChar { it.uppercase() },
                        shown.seasonYear?.toString()).joinToString(" • "), color = Violet, fontSize = 11.sp)
                    Text("${shown.episodes?.let { "${it} episodes" } ?: "Episode count unknown"} • ${shown.score?.let { "${it}% score" } ?: "Not rated"}",
                        color = Muted, fontSize = 11.sp)
                    if (shown.genres.isNotEmpty()) Text(shown.genres.joinToString(" • "), color = Violet, fontSize = 11.sp)
                    if (shown.studios.isNotEmpty()) Text("Studio: ${shown.studios.joinToString()}", color = Muted, fontSize = 11.sp)
                    Text(shown.description.ifBlank { "No synopsis available." }, color = Muted, fontSize = 12.sp)
                    if (detailLoading) CircularProgressIndicator(color = Violet, modifier = Modifier.size(20.dp))
                    detailError?.let { Text(it, color = Muted, fontSize = 10.sp) }
                    Divider(color = Panel)
                    Text("FRANCHISE MAP", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("Connected anime across sequels, prequels, movies, OVAs, specials, and side stories.",
                        color = Muted, fontSize = 10.sp)
                    if (franchiseLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(color = Violet, modifier = Modifier.size(18.dp))
                            Text("Checking connected entries…", color = Muted, fontSize = 11.sp)
                        }
                    }
                    franchiseError?.let { errorText ->
                        Text(errorText, color = Muted, fontSize = 10.sp)
                        TextButton(onClick = { franchiseRetry++ }, enabled = !franchiseLoading) {
                            Text("Retry franchise scan", color = Violet)
                        }
                    }
                    franchise?.let { graph ->
                        if (graph.isPartial) {
                            Text("Some AniList requests failed or the scan reached its limit. Showing the entries retrieved so far.",
                                color = Muted, fontSize = 10.sp)
                            TextButton(onClick = { franchiseRetry++ }, enabled = !franchiseLoading) {
                                Text("Retry to find more", color = Violet)
                            }
                        }
                        Text("${graph.entries.size} connected entries found • ${graph.scannedCount} checked",
                            color = Muted, fontSize = 10.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Display order", color = Muted, fontSize = 10.sp)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { franchiseSortByYear = !franchiseSortByYear }) {
                                Text(if (franchiseSortByYear) "Release year" else "Relationship", color = Violet, fontSize = 10.sp)
                            }
                        }
                        val orderedEntries = if (franchiseSortByYear) graph.entries.sortedWith(
                            compareBy<AnimeRelation>({ it.anime.seasonYear ?: 9999 }, { it.anime.title.lowercase() })
                        ) else graph.entries
                        orderedEntries.forEach { relation ->
                            Row(Modifier.fillMaxWidth().clickable { selectedAnime = relation.anime }
                                .background(Panel, RoundedCornerShape(10.dp)).padding(9.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                AsyncImage(model = relation.anime.cover, contentDescription = null,
                                    modifier = Modifier.size(width = 42.dp, height = 56.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(relation.anime.title, color = White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${relation.relation.replace('_',' ').lowercase().replaceFirstChar { it.uppercase() }} • ${relation.anime.format}",
                                        color = Violet, fontSize = 10.sp)
                                    Text(listOfNotNull(relation.anime.seasonYear?.toString(), relation.anime.status.lowercase().replace('_',' ')).joinToString(" • "),
                                        color = Muted, fontSize = 9.sp)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Muted)
                            }
                        }
                        if (graph.isPartial) Text("This map may be incomplete. AniList's relationship data can be missing, and the scan has a size limit.",
                            color = Muted, fontSize = 10.sp)
                        else if (graph.entries.isEmpty()) Text("AniList currently lists no connected anime for this title.",
                            color = Muted, fontSize = 10.sp)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { onToggleSaved(shown) }) {
                Text(if (savedIds.contains(shown.id)) "Remove from library" else "Add to library", color = Violet)
            } },
            dismissButton = { TextButton(onClick = { selectedAnime = null }) { Text("Close", color = Muted) } })
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryScreen(modifier: Modifier = Modifier, library: List<Anime>, onRemove: (Anime) -> Unit, onUpdate: (Anime) -> Unit) {
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }
    val filters = listOf("ALL", "WATCHING", "PLANNING", "COMPLETED", "PAUSED", "DROPPED")
    val visible = library.filter { anime ->
        (statusFilter == "ALL" || anime.watchStatus == statusFilter) &&
            (query.isBlank() || anime.title.contains(query, ignoreCase = true))
    }
    val watching = library.count { it.watchStatus == "WATCHING" }
    val completed = library.count { it.watchStatus == "COMPLETED" }
    Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(18.dp))
        Text("YOUR COLLECTION", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Your library", color = White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text("Everything you're watching, finished, or saving for later.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            DashboardStat("SAVED", library.size.toString(), Icons.Default.Bookmarks, Modifier.weight(1f))
            DashboardStat("WATCHING", watching.toString(), Icons.Default.PlayCircle, Modifier.weight(1f))
            DashboardStat("COMPLETED", completed.toString(), Icons.Default.CheckCircle, Modifier.weight(1f))
        }
        Spacer(Modifier.height(13.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Find something in your library…", color = Muted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Muted) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                Icon(Icons.Default.Close, contentDescription = "Clear search", tint = Muted)
            } },
            singleLine = true, shape = RoundedCornerShape(15.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Violet, unfocusedBorderColor = Panel2,
                focusedTextColor = White, unfocusedTextColor = White, cursorColor = Violet
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            filters.forEach { filter ->
                FilterChip(
                    selected = statusFilter == filter,
                    onClick = { statusFilter = filter },
                    label = { Text(if (filter == "ALL") "All titles" else filter.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Violet.copy(alpha = .18f),
                        selectedLabelColor = Violet,
                        labelColor = Muted
                    )
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${visible.size} ${if (visible.size == 1) "TITLE" else "TITLES"}", color = Violet,
                fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.weight(1f))
            Text("Tap + / − to update episodes", color = Muted, fontSize = 10.sp)
        }
        Spacer(Modifier.height(7.dp))
        if (visible.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().weight(1f).background(Panel, RoundedCornerShape(20.dp)).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(if (library.isEmpty()) Icons.Default.Bookmarks else Icons.Default.SearchOff,
                    contentDescription = null, tint = Violet, modifier = Modifier.size(34.dp))
                Text(if (library.isEmpty()) "Your collection starts here." else "No matching titles.",
                    color = White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    if (library.isEmpty()) "Explore anime and save titles to build your own collection."
                    else "Try another title or switch to a different status filter.",
                    color = Muted, fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(visible, key = { it.id }) { anime ->
                    Column(
                        Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(17.dp)).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                            AsyncImage(
                                model = anime.cover, contentDescription = anime.title,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.width(66.dp).height(88.dp).background(Panel2, RoundedCornerShape(10.dp))
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(anime.title, color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2)
                                Text(anime.watchStatus.replace('_', ' '), color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .6.sp)
                                Text(
                                    if (anime.episodes != null) "${anime.progress} / ${anime.episodes} episodes"
                                    else "${anime.progress} episodes tracked",
                                    color = Muted, fontSize = 11.sp
                                )
                                if (anime.episodes != null && anime.episodes > 0) {
                                    LinearProgressIndicator(
                                        progress = { (anime.progress.toFloat() / anime.episodes).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(5.dp),
                                        color = Violet, trackColor = Panel2
                                    )
                                }
                            }
                            IconButton(onClick = { onRemove(anime) }) {
                                Icon(Icons.Default.BookmarkRemove, contentDescription = "Remove ${anime.title}", tint = Muted)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("EPISODES WATCHED", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
                            Spacer(Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    val newProgress = (anime.progress - 1).coerceAtLeast(0)
                                    val newStatus = if (anime.watchStatus == "COMPLETED" && anime.episodes != null && newProgress < anime.episodes) "WATCHING" else anime.watchStatus
                                    onUpdate(anime.copy(progress = newProgress, watchStatus = newStatus, lastWatchedAt = System.currentTimeMillis()))
                                },
                                enabled = anime.progress > 0
                            ) { Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease episode", tint = Violet) }
                            Text(anime.progress.toString(), color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            IconButton(
                                onClick = {
                                    val next = anime.progress + 1
                                    val capped = anime.episodes?.let { next.coerceAtMost(it) } ?: next
                                    onUpdate(anime.copy(
                                        progress = capped,
                                        watchStatus = if (anime.episodes != null && capped >= anime.episodes) "COMPLETED" else "WATCHING",
                                        lastWatchedAt = System.currentTimeMillis()
                                    ))
                                },
                                enabled = anime.episodes == null || anime.progress < anime.episodes
                            ) { Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase episode", tint = Violet) }
                        }
                        var statusExpanded by remember(anime.id) { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { statusExpanded = true }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                                Text("Change watch status", color = Violet, fontSize = 11.sp)
                                Icon(Icons.Default.ExpandMore, contentDescription = null, tint = Violet)
                            }
                            DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                                filters.drop(1).forEach { status ->
                                    DropdownMenuItem(
                                        text = { Text(status.replace('_', ' ')) },
                                        onClick = {
                                            statusExpanded = false
                                            val fill = if (status == "COMPLETED" && anime.episodes != null) anime.episodes else anime.progress
                                            onUpdate(anime.copy(watchStatus = status, progress = fill, lastWatchedAt = System.currentTimeMillis()))
                                        }
                                    )
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
private fun MusicScreen(modifier: Modifier = Modifier) {
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

private data class MusicLink(val title: String, val url: String, val source: String)

private fun loadMusicLinks(context: Context): List<MusicLink> =
    readJsonArray(context, "musicLinks")?.mapSafe { o ->
        MusicLink(o.getString("title"), o.getString("url"), o.getString("source"))
    } ?: emptyList()

private fun persistMusicLinks(context: Context, links: List<MusicLink>) {
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

private data class QuestData(val id: String, val title: String, val detail: String,
    val progress: Int, val target: Int, val reward: Int)

@Composable
private fun QuestScreen(context: Context, library: List<Anime>,
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

private data class LocalTrack(val uri: String, val title: String)

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

private fun loadTracks(context: Context): List<LocalTrack> =
    readJsonArray(context, "tracks")?.mapSafe { item ->
        LocalTrack(item.getString("uri"), item.optString("title", "Audio track").ifBlank { "Audio track" })
    } ?: emptyList()

private fun persistTracks(context: Context, tracks: List<LocalTrack>) {
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
