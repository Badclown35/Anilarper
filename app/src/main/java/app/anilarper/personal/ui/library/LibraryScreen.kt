package app.anilarper.personal.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.anilarper.personal.theme.*
import coil.compose.AsyncImage
import app.anilarper.personal.Anime
import app.anilarper.personal.ui.home.DashboardStat

@Composable
internal fun AnimeRow(anime: Anime, saved: Boolean, onClick: () -> Unit) {
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
fun LibraryScreen(modifier: Modifier = Modifier, library: List<Anime>, onRemove: (Anime) -> Unit, onUpdate: (Anime) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf("ALL") }
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

