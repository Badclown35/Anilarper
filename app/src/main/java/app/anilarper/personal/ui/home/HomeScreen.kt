package app.anilarper.personal.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.anilarper.personal.Anime
import app.anilarper.personal.theme.*
import coil.compose.AsyncImage

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
    val favorites = library.filter { it.isFavorite }.sortedBy { it.title.lowercase() }
    val featured = watching.firstOrNull() ?: library.firstOrNull()
    val heroShape = RoundedCornerShape(28.dp)
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        // ── Header ────────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ANILARPER", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Text("Discover your next chapter.", color = White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
            Box(
                Modifier.size(48.dp)
                    .border(2.dp, Brush.sweepGradient(listOf(Violet, Sakura, Aqua, Violet)), CircleShape)
                    .padding(4.dp).clip(CircleShape).background(Panel2).clickable { onOpenProfile() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = "Profile", tint = Violet)
            }
        }
        // ── Hero ──────────────────────────────────────────────────────────────
        Box(
            Modifier.fillMaxWidth().height(300.dp).clip(heroShape).background(Panel2)
                .border(1.dp, Brush.linearGradient(listOf(Violet.copy(alpha = .7f), Color.Transparent, Sakura.copy(alpha = .5f))), heroShape)
        ) {
            if (featured != null && featured.cover.isNotBlank()) {
                AsyncImage(
                    model = featured.cover, contentDescription = featured.title,
                    contentScale = ContentScale.Crop, alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Violet.copy(alpha = .45f), Color(0xFF1B1840), Ink))))
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(alpha = .55f), Ink.copy(alpha = .96f)))))
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Violet.copy(alpha = .28f), Color.Transparent))))
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Pill(if (watching.isNotEmpty()) "NOW WATCHING" else "YOUR ANIME SPACE")
                Text(
                    if (watching.isNotEmpty()) featured?.title ?: "Continue your story"
                    else if (featured != null) "Keep your collection growing."
                    else "Find something worth watching.",
                    color = White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, lineHeight = 29.sp
                )
                if (watching.isNotEmpty() && featured != null) {
                    val total = featured.episodes
                    val fraction = if (total != null && total > 0) (featured.progress.toFloat() / total).coerceIn(0f, 1f) else 0f
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("EP ${featured.progress}" + (total?.let { " / $it" } ?: ""),
                            color = Color(0xFFE0DEEA), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        GradientProgress(fraction, Modifier.weight(1f))
                    }
                } else {
                    Text("Explore anime, save favorites, and make this space yours.",
                        color = Color(0xFFE0DEEA), fontSize = 12.sp, maxLines = 2)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Button(
                        onClick = { if (watching.isNotEmpty()) onOpenLibrary() else onDiscover() },
                        colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(5.dp))
                        Text(if (watching.isNotEmpty()) "Open library" else "Explore anime", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onOpenStats, shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, White.copy(alpha = .4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = White)
                    ) { Text("My stats") }
                }
            }
        }
        // ── Continue watching ─────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Continue watching", "Jump back into your current shows", "See all", onOpenLibrary)
            if (watching.isEmpty()) {
                Row(
                    Modifier.fillMaxWidth().animeCard(RoundedCornerShape(18.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = Violet, modifier = Modifier.size(32.dp))
                    Column {
                        Text("Nothing playing right now", color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Set a title to Watching and it will appear here.", color = Muted, fontSize = 11.sp)
                    }
                }
            } else {
                watching.take(5).forEach { anime ->
                    val total = anime.episodes
                    val fraction = if (total != null && total > 0) (anime.progress.toFloat() / total).coerceIn(0f, 1f) else 0f
                    Row(
                        Modifier.fillMaxWidth().animeCard(RoundedCornerShape(18.dp))
                            .clickable { onOpenLibrary() }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PosterImage(anime.cover, anime.title, Modifier.size(58.dp, 80.dp), corner = 12.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(anime.title, color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2)
                            Pill("EP ${anime.progress}" + (total?.let { " / $it" } ?: ""))
                            GradientProgress(fraction, Modifier.fillMaxWidth())
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Muted)
                    }
                }
            }
        }
        // ── Stats ─────────────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Your collection", "A snapshot of what you've been up to")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardStat("SAVED", library.size.toString(), Icons.Default.Bookmarks, Modifier.weight(1f))
                DashboardStat("WATCHING", watching.size.toString(), Icons.Default.PlayCircle, Modifier.weight(1f))
                DashboardStat("FINISHED", completed.size.toString(), Icons.Default.CheckCircle, Modifier.weight(1f))
            }
        }
        // ── Recently completed ────────────────────────────────────────────────
        if (completed.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Recently completed", "Your latest finished stories", "Library", onOpenLibrary)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    completed.take(8).forEach { anime ->
                        Column(
                            Modifier.width(118.dp).clickable { onOpenLibrary() },
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            PosterImage(anime.cover, anime.title, Modifier.fillMaxWidth().height(158.dp), corner = 16.dp)
                            Text(anime.title, color = White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                            Pill("COMPLETED", color = Aqua)
                        }
                    }
                }
            }
        }
        // ── Favorites ─────────────────────────────────────────────────────────
        if (favorites.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Your favorites", "The titles you marked to keep close", "Library", onOpenLibrary)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    favorites.take(10).forEach { anime ->
                        Column(Modifier.width(118.dp).clickable { onOpenLibrary() }, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Box {
                                PosterImage(anime.cover, anime.title, Modifier.fillMaxWidth().height(158.dp), corner = 16.dp)
                                Icon(Icons.Default.Star, contentDescription = "Favorite", tint = Color(0xFFFFD36A), modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
                            }
                            Text(anime.title, color = White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        }
                    }
                }
            }
        }
        // ── Explore ───────────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Explore Anilarper", "Your library, reading list, and goals")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("Discover", "Find your next series", Icons.Default.Explore, Modifier.weight(1f), onDiscover)
                QuickTile("Manga & novels", "Your reading shelf", Icons.Default.MenuBook, Modifier.weight(1f), onOpenRead)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("Quests", "Milestones and XP", Icons.Default.EmojiEvents, Modifier.weight(1f), onOpenQuests)
                QuickTile("Statistics", "Your activity", Icons.Default.Insights, Modifier.weight(1f), onOpenStats)
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Brush.horizontalGradient(listOf(Violet.copy(alpha = .3f), Sakura.copy(alpha = .18f))))
                .border(1.dp, Violet.copy(alpha = .4f), RoundedCornerShape(20.dp))
                .clickable { onOpenProfile() }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = White, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text("Make it yours", color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Pick your accent color and customize your profile", color = Color(0xFFE0DEEA), fontSize = 11.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = White)
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
internal fun DashboardStat(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier.animeCard(RoundedCornerShape(18.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(30.dp).background(Violet.copy(alpha = .18f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Violet, modifier = Modifier.size(17.dp))
        }
        Text(value, color = White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .9.sp)
    }
}

@Composable
private fun QuickTile(
    title: String, subtitle: String, icon: ImageVector,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Column(
        modifier.animeCard(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(38.dp).background(
                Brush.linearGradient(listOf(Violet.copy(alpha = .35f), Sakura.copy(alpha = .2f))), RoundedCornerShape(12.dp)
            ),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = Violet, modifier = Modifier.size(21.dp)) }
        Text(title, color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 14.sp)
    }
}
