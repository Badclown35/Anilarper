package app.anilarper.personal.ui.extras

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.anilarper.personal.Anime
import app.anilarper.personal.AniListApi
import app.anilarper.personal.AnimeFilters
import app.anilarper.personal.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class MalRow(val title: String, val score: Int, val status: String, val progress: Int, val total: Int?)
private data class MalImportResult(val imported: Int, val skipped: Int, val errors: Int)

/** Import a MyAnimeList anime-list CSV export and resolve titles to AniList IDs. */
@Composable
fun MalImportScreen(onImport: (Anime) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<MalRow>>(emptyList()) }
    var running by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("Choose your anime-list export CSV from MyAnimeList.") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val parsed = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { parseMalCsv(it.readText()) }
                        ?: error("Couldn't read that file.")
                }
            }
            parsed.onSuccess { rows = it; message = "${it.size} entries ready. Import resolves titles against AniList." }
                .onFailure { message = it.message ?: "CSV couldn't be parsed." }
        }
    }
    Column(modifier.fillMaxSize().padding(18.dp)) {
        Text("TRANSFER YOUR LIST", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text("MAL import", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        Text("Import an anime CSV export; titles are matched to AniList IDs.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { picker.launch(arrayOf("text/*", "text/csv", "application/csv")) }, enabled = !running,
            colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink)) {
            Icon(Icons.Default.FileOpen, contentDescription = null); Text("Choose MAL CSV")
        }
        Text(message, color = Muted, fontSize = 11.sp)
        if (running) LinearProgressIndicator(progress = { if (rows.isEmpty()) 0f else progress.toFloat()/rows.size }, modifier = Modifier.fillMaxWidth(), color = Violet)
        if (rows.isNotEmpty()) Button(onClick = {
            running = true; progress = 0
            scope.launch {
                var imported = 0; var skipped = 0; var errors = 0
                rows.forEach { row ->
                    try {
                        val candidates = AniListApi.search(row.title, 1, AnimeFilters()).items
                        val target = candidates.firstOrNull { normalizeTitle(it.title) == normalizeTitle(row.title) }
                        if (target == null) skipped++ else {
                            val status = when (row.status.trim().lowercase()) {
                                "2", "completed" -> "COMPLETED"
                                "1", "watching" -> "WATCHING"
                                "3", "on-hold", "on hold" -> "PAUSED"
                                "4", "dropped" -> "DROPPED"
                                else -> "PLANNING" // MAL status 6 = Plan to Watch
                            }
                            onImport(target.copy(watchStatus = status,
                                progress = if (status == "COMPLETED" && target.episodes != null) target.episodes else row.progress,
                                score = row.score.takeIf { it > 0 } ?: target.score,
                                lastWatchedAt = System.currentTimeMillis()))
                            imported++
                        }
                    } catch (_: Exception) { errors++ }
                    progress++
                }
                running = false
                message = "Imported $imported • skipped $skipped • errors $errors. Review duplicates in your library."
            }
        }, enabled = !running, colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink)) {
            Text(if (running) "Importing…" else "Import ${rows.size} entries")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(rows.take(100), key = { it.title }) { row ->
                Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(10.dp)).padding(10.dp)) {
                    Text(row.title, color = White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("${row.progress}/${row.total ?: "?"} • ${row.status}", color = Muted, fontSize = 10.sp)
                }
            }
            if (rows.size > 100) item { Text("Previewing first 100 of ${rows.size}. All entries import.", color = Muted, fontSize = 10.sp) }
        }
    }
}

private fun normalizeTitle(value: String) = value.lowercase().replace(Regex("[^a-z0-9]"), "")

private fun parseMalCsv(raw: String): List<MalRow> {
    val records = buildList {
        var row = mutableListOf<String>(); val field = StringBuilder(); var quoted = false; var i = 0
        while (i < raw.length) {
            val ch = raw[i]
            when {
                ch == '"' && quoted && i + 1 < raw.length && raw[i + 1] == '"' -> { field.append('"'); i++ }
                ch == '"' -> quoted = !quoted
                ch == ',' && !quoted -> { row += field.toString(); field.clear() }
                (ch == '\n' || ch == '\r') && !quoted -> {
                    row += field.toString(); field.clear()
                    if (row.any { it.isNotBlank() }) add(row)
                    row = mutableListOf()
                    if (ch == '\r' && i + 1 < raw.length && raw[i + 1] == '\n') i++
                }
                else -> field.append(ch)
            }
            i++
        }
        row += field.toString()
        if (row.any { it.isNotBlank() }) add(row)
    }
    if (records.size < 2) error("CSV is empty.")
    val headers = records.first().map { it.trim().removePrefix("\uFEFF").lowercase() }
    fun index(vararg names: String) = names.firstNotNullOfOrNull { headers.indexOf(it).takeIf { n -> n >= 0 } } ?: -1
    val titleI = index("anime_title", "title")
    val scoreI = index("my_score")
    val statusI = index("my_status")
    val progressI = index("my_watched_episodes")
    val totalI = index("anime_episodes")
    if (titleI < 0) error("Missing anime_title column. Use MyAnimeList's anime-list CSV export.")
    return records.drop(1).mapNotNull { row ->
        val title = row.getOrNull(titleI)?.trim().orEmpty()
        if (title.isBlank()) null else MalRow(title,
            row.getOrNull(scoreI)?.toIntOrNull() ?: 0,
            row.getOrNull(statusI)?.trim().orEmpty(),
            row.getOrNull(progressI)?.toIntOrNull() ?: 0,
            row.getOrNull(totalI)?.toIntOrNull()?.takeIf { it > 0 })
    }
}
