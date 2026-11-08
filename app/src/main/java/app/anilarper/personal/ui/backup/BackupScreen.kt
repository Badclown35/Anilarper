package app.anilarper.personal.ui.backup

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import app.anilarper.personal.theme.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import app.anilarper.personal.data.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackupScreen(context: Context, modifier: Modifier = Modifier, onRestored: () -> Unit = {}) {
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val repository = remember(context) { LibraryRepository(context.applicationContext) }
    var confirmRestore by remember { mutableStateOf<Uri?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val backup = withContext(Dispatchers.IO) { repository.createBackup() }
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(backup.toString(2)) }
                        ?: error("Couldn't open the backup file for writing.")
                }.onSuccess { message = "Backup exported successfully." }
                    .onFailure { message = it.message ?: "Backup export failed." }
            }
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
                    scope.launch {
                        runCatching {
                            val raw = withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            } ?: error("Couldn't read the selected file.")
                            val backup = JSONObject(raw)
                            require(backup.optString("format") == "anilarper-backup") { "This isn't an Anilarper backup file." }
                            require(backup.optInt("version", 0) == 1) { "Unsupported backup version." }
                            val values = backup.getJSONObject("preferences")
                            require(values.length() > 0) { "This backup contains no data. Nothing was changed." }
                            val snapshot = withContext(Dispatchers.IO) { repository.createBackup() }
                            withContext(Dispatchers.IO) {
                                java.io.File(context.filesDir, "pre-restore-backup.json").writeText(snapshot.toString(2))
                            }
                            val prefs = context.getSharedPreferences("anilarper", Context.MODE_PRIVATE)
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
                            check(withContext(Dispatchers.IO) { editor.commit() }) { "Couldn't save the restored data." }
                        }.onSuccess { onRestored(); message = "Backup restored." }
                            .onFailure { message = it.message ?: "Restore failed. Your data may not have changed." }
                    }
                }) { Text("Restore", color = Violet) }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = null }) { Text("Cancel", color = Muted) } }
        )
    }
}

