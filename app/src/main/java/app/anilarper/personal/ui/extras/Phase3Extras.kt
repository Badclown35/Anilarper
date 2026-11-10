package app.anilarper.personal.ui.extras

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.anilarper.personal.Anime
import app.anilarper.personal.AnimeFilters
import app.anilarper.personal.AniListApi
import app.anilarper.personal.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private data class AnimeCustomList(val name: String, val ids: Set<Int>)

private class CustomListStore(context: Context) {
    private val prefs = context.getSharedPreferences("anilarper", Context.MODE_PRIVATE)
    fun load(): List<AnimeCustomList> = runCatching {
        val a = JSONArray(prefs.getString("customAnimeLists", "[]"))
        (0 until a.length()).mapNotNull { i ->
            runCatching {
                val o = a.getJSONObject(i)
                val ids = o.optJSONArray("ids") ?: JSONArray()
                AnimeCustomList(o.getString("name"), (0 until ids.length()).map { ids.getInt(it) }.toSet())
            }.getOrNull()
        }
    }.getOrDefault(emptyList())
    fun save(lists: List<AnimeCustomList>) {
        prefs.edit().putString("customAnimeLists", JSONArray().apply {
            lists.forEach { list -> put(JSONObject().put("name", list.name).put("ids", JSONArray(list.ids))) }
        }.toString()).apply()
    }
}

@Composable
fun CustomListsScreen(library: List<Anime>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember(context) { CustomListStore(context) }
    var lists by remember { mutableStateOf(store.load()) }
    var newName by rememberSaveable { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxSize().padding(18.dp)) {
        Text("YOUR COLLECTION, YOUR RULES", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text("Custom lists", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        Text("Make watchlists, favorites, seasonal picks, or anything you want.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        Button(onClick = { showCreate = true }, colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink)) {
            Icon(Icons.Default.Add, contentDescription = null); Text("Create list")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(lists, key = { it.name }) { list ->
                Column(Modifier.fillMaxWidth().animeCard(RoundedCornerShape(15.dp)).padding(13.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(list.name, color = White, fontWeight = FontWeight.SemiBold)
                            Text("${list.ids.size} titles", color = Muted, fontSize = 11.sp)
                        }
                        IconButton(onClick = { selected = if (selected == list.name) null else list.name }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit list", tint = Violet)
                        }
                        IconButton(onClick = { lists = lists.filterNot { it.name == list.name }; store.save(lists) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete list", tint = Muted)
                        }
                    }
                    if (selected == list.name) {
                        library.forEach { anime ->
                            Row(Modifier.fillMaxWidth().clickable {
                                val updated = list.copy(ids = if (anime.id in list.ids) list.ids - anime.id else list.ids + anime.id)
                                lists = lists.map { if (it.name == list.name) updated else it }; store.save(lists)
                            }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = anime.id in list.ids, onCheckedChange = { checked ->
                                    val updated = list.copy(ids = if (checked) list.ids + anime.id else list.ids - anime.id)
                                    lists = lists.map { if (it.name == list.name) updated else it }; store.save(lists)
                                })
                                Text(anime.title, color = White, fontSize = 12.sp)
                            }
                        }
                        if (library.isEmpty()) Text("Add anime to your library first to populate lists.", color = Muted, fontSize = 11.sp)
                    }
                }
            }
            if (lists.isEmpty()) item { Text("No custom lists yet. Create one above.", color = Muted) }
        }
    }
    if (showCreate) AlertDialog(
        onDismissRequest = { showCreate = false }, containerColor = Panel,
        title = { Text("New custom list", color = White) },
        text = { OutlinedTextField(newName, { newName = it.take(40) }, label = { Text("List name") }, singleLine = true) },
        confirmButton = { TextButton(onClick = {
            val name = newName.trim()
            if (name.isNotEmpty() && lists.none { it.name.equals(name, true) }) {
                lists = lists + AnimeCustomList(name, emptySet()); store.save(lists)
            }
            newName = ""; showCreate = false
        }) { Text("Create", color = Violet) } },
        dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel", color = Muted) } }
    )
}

@Composable
fun RecommendationsScreen(library: List<Anime>, onAdd: (Anime) -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var results by remember { mutableStateOf<List<Anime>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Build recommendations from the genres in your library.") }
    val genres = library.flatMap { it.genres }.groupingBy { it }.eachCount().entries
        .sortedByDescending { it.value }.take(5).map { it.key }
    fun load() {
        if (loading) return
        loading = true; message = "Finding titles…"
        scope.launch {
            runCatching {
                val genre = genres.firstOrNull() ?: "Fantasy"
                AniListApi.search("", 1, AnimeFilters(genre = genre, sort = "POPULARITY_DESC")).items
                    .filterNot { candidate -> library.any { it.id == candidate.id } }.take(20)
            }.onSuccess { results = it; message = if (it.isEmpty()) "No new matches found. Try again later." else "Inspired by ${genres.firstOrNull() ?: "popular anime"} in your collection." }
                .onFailure { message = "Couldn't load recommendations. Check your connection." }
            loading = false
        }
    }
    Column(modifier.fillMaxSize().padding(18.dp)) {
        Text("DISCOVER YOUR NEXT", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text("Recommendations", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        Text(message, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Button(onClick = { load() }, enabled = !loading, colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink)) {
            if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(6.dp)); Text("Refresh picks")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(results, key = { it.id }) { anime ->
                Row(Modifier.fillMaxWidth().animeCard(RoundedCornerShape(14.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box(Modifier.width(60.dp).height(84.dp)) {
                        coil.compose.AsyncImage(model = anime.cover, contentScale = androidx.compose.ui.layout.ContentScale.Crop, contentDescription = anime.title, modifier = Modifier.fillMaxSize())
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(anime.title, color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(anime.genres.take(3).joinToString(" • "), color = Muted, fontSize = 10.sp)
                        Text("★ ${anime.score ?: "—"}", color = Violet, fontSize = 10.sp)
                    }
                    IconButton(onClick = { onAdd(anime); results = results.filterNot { it.id == anime.id } }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add to library", tint = Violet)
                    }
                }
            }
        }
    }
}
