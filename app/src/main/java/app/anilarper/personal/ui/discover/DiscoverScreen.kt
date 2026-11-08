package app.anilarper.personal.ui.discover

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
import app.anilarper.personal.ui.library.AnimeRow
import app.anilarper.personal.data.DiscoverViewModel
import app.anilarper.personal.data.CatalogCacheRepository
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.runtime.saveable.rememberSaveable
import app.anilarper.personal.*
import app.anilarper.personal.ui.library.AnimeRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(modifier: Modifier = Modifier, savedIds: Set<Int>, viewModel: DiscoverViewModel, onToggleSaved: (Anime) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val cache by viewModel.cache.collectAsState()
    val results = cache.items
    val hasNext = cache.hasNextPage
    var loading by remember { mutableStateOf(cache.items.isEmpty()) }
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
    var franchiseSortByYear by rememberSaveable { mutableStateOf(false) }
    var genre by rememberSaveable { mutableStateOf<String?>(null) }
    var format by rememberSaveable { mutableStateOf<String?>(null) }
    var status by rememberSaveable { mutableStateOf<String?>(null) }
    var season by rememberSaveable { mutableStateOf<String?>(null) }
    var seasonYear by rememberSaveable { mutableIntStateOf(2026) }
    var sort by rememberSaveable { mutableStateOf("TRENDING_DESC") }
    var filterMenu by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val catalogCache = remember(context) { CatalogCacheRepository(context) }
    val keyboard = LocalSoftwareKeyboardController.current
    var loadJob by remember { mutableStateOf<Job?>(null) }

    fun load(reset: Boolean = true) {
        loadJob?.cancel()
        loadJob = scope.launch {
            if (reset) loading = true else loadingMore = true
            error = null
            try {
                val nextPage = if (reset) 1 else viewModel.cache.value.page + 1
                val filters = AnimeFilters(genre, format, status, season, if (season == null) null else seasonYear, sort)
                val cacheKey = CatalogCacheRepository.animeKey(query, nextPage, filters)
                val fetched = AniListApi.search(query, nextPage, filters)
                catalogCache.saveAnimePage(cacheKey, fetched)
                val merged = if (reset) fetched.items else (viewModel.cache.value.items + fetched.items).distinctBy { it.id }
                viewModel.updateCache(merged, fetched.page, fetched.hasNextPage)
            } catch (e: Exception) {
                val nextPage = if (reset) 1 else viewModel.cache.value.page + 1
                val filters = AnimeFilters(genre, format, status, season, if (season == null) null else seasonYear, sort)
                val cacheKey = CatalogCacheRepository.animeKey(query, nextPage, filters)
                val cached = runCatching { catalogCache.getAnimePage(cacheKey) }.getOrNull()
                if (cached != null) {
                    val merged = if (reset) cached.items else (viewModel.cache.value.items + cached.items).distinctBy { it.id }
                    viewModel.updateCache(merged, cached.page, cached.hasNextPage)
                    error = "Offline — showing saved catalog results."
                } else {
                    error = if (e.message?.contains("rate", true) == true)
                        "AniList is receiving too many requests. Wait a moment, then retry."
                    else "Couldn't load anime. Check your connection and try again."
                }
            } finally { if (isActive) { loading = false; loadingMore = false } }
        }
    }
    LaunchedEffect(Unit) { if (viewModel.cache.value.items.isEmpty()) load() }
    DisposableEffect(Unit) { onDispose { loadJob?.cancel() } }
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

