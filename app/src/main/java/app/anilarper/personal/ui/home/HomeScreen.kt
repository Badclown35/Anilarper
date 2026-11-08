package app.anilarper.personal.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import app.anilarper.personal.theme.*
import coil.compose.AsyncImage
import app.anilarper.personal.Anime
import app.anilarper.personal.QuickTile

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    library: List<Anime>,
    onDiscover: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenRead: () -> Unit,
    onOpenQuests: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val watching = library.filter { it.watchStatus == "WATCHING" }.sortedByDescending { it.lastWatchedAt }
    val completed = library.filter { it.watchStatus == "COMPLETED" }.sortedByDescending { it.lastWatchedAt }
    val featured = watching.firstOrNull() ?: library.firstOrNull()
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ANILARPER", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.2.sp)
                Text("Discover your next chapter.", color = White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.size(44.dp).background(Panel2, CircleShape).clickable { onOpenProfile() },
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = "Profile", tint = Violet)
            }
        }
        Box(Modifier.fillMaxWidth().height(270.dp).background(Panel2, RoundedCornerShape(24.dp))) {
            if (featured != null && featured.cover.isNotBlank()) {
                AsyncImage(model = featured.cover, contentDescription = featured.title,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(
                    listOf(Color(0xFF40336F), Color(0xFF201D36), Color(0xFF11131B)))))
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
                Color.Transparent, Color(0xF5090A10)))))
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (watching.isNotEmpty()) "WELCOME BACK" else "YOUR PERSONAL ANIME SPACE",
                    color = Color(0xFFD2C9FF), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                Text(if (watching.isNotEmpty()) featured?.title ?: "Continue your story"
                    else if (featured != null) "Keep your collection growing."
                    else "Find something worth watching.",
                    color = White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2)
                Text(if (watching.isNotEmpty()) "Pick up where you left off."
                    else "Explore anime, save favorites, and make this space yours.",
                    color = Color(0xFFE0DEEA), fontSize = 12.sp, maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Button(onClick = { if (watching.isNotEmpty()) onOpenLibrary() else onDiscover() },
                        colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink),
                        shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(5.dp))
                        Text(if (watching.isNotEmpty()) "Open library" else "Explore anime", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = onOpenStats, shape = RoundedCornerShape(12.dp)) {
                        Text("My stats", color = White)
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Continue watching", color = White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text("Jump back into your current shows", color = Muted, fontSize = 11.sp)
                }
                TextButton(onClick = onOpenLibrary) { Text("See all", color = Violet) }
            }
            if (watching.isEmpty()) {
                Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(17.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = Violet, modifier = Modifier.size(30.dp))
                    Column {
                        Text("Nothing playing right now", color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Set a title to Watching and it will appear here.", color = Muted, fontSize = 11.sp)
                    }
                }
            } else {
                watching.take(5).forEach { anime ->
                    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp))
                        .clickable { onOpenLibrary() }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AsyncImage(model = anime.cover, contentDescription = anime.title,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.size(54.dp, 70.dp).background(Panel2, RoundedCornerShape(9.dp)))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(anime.title, color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2)
                            Text("EPISODE " + anime.progress + (anime.episodes?.let { " / " + it } ?: ""),
                                color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
                            val fraction = if (anime.episodes != null && anime.episodes > 0)
                                (anime.progress.toFloat() / anime.episodes).coerceIn(0f, 1f) else 0f
                            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = Violet, trackColor = Panel2)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Muted)
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Column {
                Text("Your collection", color = White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("A snapshot of what you've been up to", color = Muted, fontSize = 11.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                DashboardStat("SAVED", library.size.toString(), Icons.Default.Bookmarks, Modifier.weight(1f))
                DashboardStat("WATCHING", watching.size.toString(), Icons.Default.PlayCircle, Modifier.weight(1f))
                DashboardStat("FINISHED", completed.size.toString(), Icons.Default.CheckCircle, Modifier.weight(1f))
            }
        }
        if (completed.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Recently completed", color = White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("Your latest finished stories", color = Muted, fontSize = 11.sp)
                    }
                    TextButton(onClick = onOpenLibrary) { Text("Library", color = Violet) }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    completed.take(3).forEach { anime ->
                        Column(Modifier.width(112.dp).clickable { onOpenLibrary() },
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AsyncImage(model = anime.cover, contentDescription = anime.title,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(145.dp).background(Panel2, RoundedCornerShape(12.dp)))
                            Text(anime.title, color = White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                            Text("COMPLETED", color = Violet, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text("Explore Anilarper", color = White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("Your library, reading list, and goals", color = Muted, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("Discover", "Find your next series", Icons.Default.Explore, Modifier.weight(1f), onDiscover)
                QuickTile("Manga & novels", "Your reading shelf", Icons.Default.MenuBook, Modifier.weight(1f), onOpenRead)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("Quests", "Milestones and XP", Icons.Default.EmojiEvents, Modifier.weight(1f), onOpenQuests)
                QuickTile("Statistics", "Your activity", Icons.Default.Insights, Modifier.weight(1f), onOpenStats)
            }
        }
        Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Panel2, Panel)),
            RoundedCornerShape(17.dp)).clickable { onOpenProfile() }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = Violet, modifier = Modifier.size(23.dp))
            Column(Modifier.weight(1f)) {
                Text("Make it yours", color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("Customize your profile and personal space", color = Muted, fontSize = 11.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Muted)
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
internal fun DashboardStat(label: String, value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(15.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = Violet, modifier = Modifier.size(18.dp))
        Text(value, color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
    }
}
