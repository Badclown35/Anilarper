package app.anilarper.personal.ui.extras

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.anilarper.personal.Anime
import app.anilarper.personal.AnimeFilters
import app.anilarper.personal.AniListApi
import app.anilarper.personal.theme.*
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonalScheduleScreen(
    onAdd: (Anime) -> Unit,
    savedIds: Set<Int>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var season by remember { mutableStateOf(currentSeason()) }
    var year by remember { mutableIntStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) }
    var titles by remember { mutableStateOf<List<Anime>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        if (loading) return
        loading = true
        error = null
        scope.launch {
            runCatching {
                AniListApi.search("", 1, AnimeFilters(season = season, seasonYear = year, sort = "POPULARITY_DESC"))
            }.onSuccess { titles = it.items }
                .onFailure { error = "Couldn't load this season. Check your connection and retry." }
            loading = false
        }
    }
    LaunchedEffect(season, year) { refresh() }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("AIRING & UPCOMING", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
        Text("Seasonal schedule", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        Text("Browse anime by release season.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("WINTER", "SPRING", "SUMMER", "FALL").forEach { value ->
                FilterChip(selected = season == value, onClick = { season = value }, label = {
                    Text(value.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp)
                })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { year = (year - 1).coerceAtLeast(1970) }) { Text("−") }
            Text(year.toString(), color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            OutlinedButton(onClick = { year = (year + 1).coerceAtMost(2100) }) { Text("+") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { refresh() }, enabled = !loading) { Text("Refresh", color = Violet) }
        }
        when {
            loading && titles.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Violet)
            }
            error != null && titles.isEmpty() -> Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error!!, color = Muted)
                TextButton(onClick = { refresh() }) { Text("Try again", color = Violet) }
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                item { Text("${titles.size} popular titles · ${season.lowercase().replaceFirstChar { it.uppercase() }} ${year}",
                    color = Muted, fontSize = 11.sp) }
                items(titles, key = { it.id }) { anime ->
                    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(15.dp)).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                        AsyncImage(model = anime.cover, contentDescription = anime.title,
                            modifier = Modifier.size(64.dp, 88.dp), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(anime.title, color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                            Text(listOfNotNull(anime.format.takeIf { it.isNotBlank() }, anime.status.replace('_', ' ').lowercase()
                                .replaceFirstChar { it.uppercase() }).joinToString(" · "), color = Muted, fontSize = 10.sp)
                            Text("★ ${anime.score ?: "—"}   •   ${anime.episodes?.let { "$it eps" } ?: "Episodes TBA"}",
                                color = Violet, fontSize = 10.sp)
                        }
                        TextButton(onClick = { onAdd(anime) }, enabled = anime.id !in savedIds) {
                            Text(if (anime.id in savedIds) "Saved" else "＋", color = Violet)
                        }
                    }
                }
            }
        }
    }
}

private fun currentSeason(): String {
    return when (java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)) {
        in 0..2 -> "WINTER"
        in 3..5 -> "SPRING"
        in 6..8 -> "SUMMER"
        else -> "FALL"
    }
}
