package app.anilarper.personal.ui.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import app.anilarper.personal.theme.*
import coil.compose.AsyncImage
import app.anilarper.personal.data.ReadingSearchViewModel
import app.anilarper.personal.ReadingTitle

@Composable
fun ReadingScreen(
    modifier: Modifier,
    library: List<ReadingTitle>,
    searchViewModel: ReadingSearchViewModel,
    onAdd: (ReadingTitle) -> Unit,
    onUpdate: (ReadingTitle) -> Unit,
    onRemove: (ReadingTitle) -> Unit
) {
    var novels by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val searchState by searchViewModel.state.collectAsState()
    val results = searchState.results
    val loading = searchState.loading
    val error = searchState.error
    val keyboard = LocalSoftwareKeyboardController.current
    fun search() { searchViewModel.search(query, novels) }
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

