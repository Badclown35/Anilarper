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

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 17.dp),
        verticalArrangement = Arrangement.spacedBy(21.dp)) {
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("ANILARPER", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.8.sp)
                Text("Your anime universe.", color = White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            }
            Surface(onClick = onOpenProfile, shape = CircleShape, color = Panel2, border = BorderStroke(1.dp, Outline)) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = "Profile", tint = Violet)
                }
            }
        }
        Surface(onClick = onDiscover, shape = RoundedCornerShape(15.dp), color = Panel,
            border = BorderStroke(1.dp, Outline), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, null, tint = Violet)
                Spacer(Modifier.width(10.dp))
                Text("Search anime, genres, seasons…", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ArrowForward, "Open discovery", tint = Muted, modifier = Modifier.size(17.dp))
            }
        }

        Box(Modifier.fillMaxWidth().height(282.dp).clip(RoundedCornerShape(25.dp))
            .background(Panel2).border(1.dp, Outline, RoundedCornerShape(25.dp))) {
            if (featured != null && featured.cover.isNotBlank()) {
                AsyncImage(model = featured.cover, contentDescription = featured.title,
                    contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                    listOf(Color.Transparent, Ink.copy(alpha = .34f), Ink.copy(alpha = .97f)))))
            } else {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Panel2, Color(0xFF202B43), Ink))))
                Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Violet.copy(alpha = .28f), Color.Transparent))))
            }
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(19.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Pill(if (watching.isNotEmpty()) "PICK UP WHERE YOU LEFT OFF" else "WELCOME TO ANILARPER")
                Text(when {
                    watching.isNotEmpty() -> featured?.title ?: "Continue your story"
                    featured != null -> featured.title
                    else -> "Find your next favorite."
                }, color = White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, lineHeight = 28.sp)
                if (watching.isNotEmpty() && featured != null) {
                    val total = featured.episodes
                    val fraction = if (total != null && total > 0) featured.progress.toFloat() / total else 0f
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("EP " + featured.progress + (total?.let { " / " + it } ?: ""),
                            color = White.copy(alpha = .88f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        GradientProgress(fraction, Modifier.weight(1f))
                    }
                } else Text(if (featured != null) "Your collection is ready whenever you are."
                    else "Discover series, build your library, and track your progress.",
                    color = White.copy(alpha = .78f), fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Button(onClick = { if (watching.isNotEmpty()) onOpenLibrary() else onDiscover() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(5.dp))
                        Text(if (watching.isNotEmpty()) "My library" else "Explore anime", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = onOpenStats, shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, White.copy(alpha = .45f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = White),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)) { Text("Statistics") }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            SectionTitle("Your collection", "A quick look at your progress")
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                DashboardStat("SAVED", library.size.toString(), Icons.Default.Bookmarks, Modifier.weight(1f))
                DashboardStat("WATCHING", watching.size.toString(), Icons.Default.PlayCircle, Modifier.weight(1f))
                DashboardStat("COMPLETED", completed.size.toString(), Icons.Default.CheckCircle, Modifier.weight(1f))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            SectionTitle("Continue watching", "Your active series", "View all", onOpenLibrary)
            if (watching.isEmpty()) {
                Row(Modifier.fillMaxWidth().animeCard(RoundedCornerShape(17.dp)).padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.PlayCircleOutline, null, tint = Violet, modifier = Modifier.size(30.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Nothing in progress yet", color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Add a title and set its status to Watching.", color = Muted, fontSize = 11.sp)
                    }
                    IconButton(onClick = onDiscover) { Icon(Icons.Default.Add, "Discover anime", tint = Violet) }
                }
            } else watching.take(5).forEach { anime ->
                val total = anime.episodes
                val fraction = if (total != null && total > 0) anime.progress.toFloat() / total else 0f
                Row(Modifier.fillMaxWidth().animeCard(RoundedCornerShape(16.dp)).clickable(onClick = onOpenLibrary).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    PosterImage(anime.cover, anime.title, Modifier.size(55.dp, 74.dp), 10.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(anime.title, color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2)
                        Text("EPISODE " + anime.progress + (total?.let { " / " + it } ?: ""),
                            color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        GradientProgress(fraction, Modifier.fillMaxWidth())
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Muted)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            SectionTitle("Explore", "Jump into another part of your collection")
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                QuickTile("Discover", "Search the catalog", Icons.Default.Explore, Modifier.weight(1f), onDiscover)
                QuickTile("Reading", "Manga and novels", Icons.Default.MenuBook, Modifier.weight(1f), onOpenRead)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                QuickTile("Quests", "Milestones and XP", Icons.Default.EmojiEvents, Modifier.weight(1f), onOpenQuests)
                QuickTile("Statistics", "Your activity", Icons.Default.Insights, Modifier.weight(1f), onOpenStats)
            }
        }
        if (completed.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Recently completed", "Finished titles", "See all", onOpenLibrary)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    completed.take(8).forEach { anime ->
                        Column(Modifier.width(112.dp).clickable(onClick = onOpenLibrary), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PosterImage(anime.cover, anime.title, Modifier.fillMaxWidth().height(148.dp), 13.dp)
                            Text(anime.title, color = White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                            Pill("COMPLETED", color = Aqua)
                        }
                    }
                }
            }
        }
        if (favorites.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Your favorites", "The titles you starred", "Library", onOpenLibrary)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    favorites.take(8).forEach { anime ->
                        Column(Modifier.width(112.dp).clickable(onClick = onOpenLibrary), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box {
                                PosterImage(anime.cover, anime.title, Modifier.fillMaxWidth().height(148.dp), 13.dp)
                                Icon(Icons.Default.Star, "Favorite", tint = Color(0xFFFFD36A),
                                    modifier = Modifier.align(Alignment.TopEnd).padding(7.dp))
                            }
                            Text(anime.title, color = White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Violet.copy(alpha = .2f), Aqua.copy(alpha = .08f))))
            .border(1.dp, Outline, RoundedCornerShape(18.dp)).clickable(onClick = onOpenProfile).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Icon(Icons.Default.Tune, null, tint = Violet, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Personalize your space", color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Profile, accent color, and appearance", color = Muted, fontSize = 10.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
internal fun DashboardStat(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(modifier.animeCard(RoundedCornerShape(16.dp)).padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(28.dp).background(Violet.copy(alpha = .14f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Violet, modifier = Modifier.size(16.dp))
        }
        Text(value, color = White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = .5.sp, maxLines = 1)
    }
}

@Composable
private fun QuickTile(title: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.animeCard(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(34.dp).background(Violet.copy(alpha = .14f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Violet, modifier = Modifier.size(19.dp))
        }
        Text(title, color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(subtitle, color = Muted, fontSize = 10.sp, lineHeight = 13.sp)
    }
}
