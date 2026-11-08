package app.anilarper.personal.ui.profile

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import app.anilarper.personal.theme.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import app.anilarper.personal.*
import app.anilarper.personal.theme.*

private data class ProfileSettings(
    val displayName: String = "Anime Explorer",
    val bio: String = "Finding my next favorite story.",
    val avatarUrl: String = "",
    val bannerStyle: Int = 0,
    val favoriteTitles: String = "",
    val accentStyle: Int = 0
)

private fun loadProfile(context: Context): ProfileSettings {
    val p = context.getSharedPreferences("anilarper", Context.MODE_PRIVATE)
    return ProfileSettings(
        p.getString("profileName", "Anime Explorer") ?: "Anime Explorer",
        p.getString("profileBio", "Finding my next favorite story.") ?: "",
        p.getString("profileAvatar", "") ?: "",
        p.getInt("profileBanner", 0),
        p.getString("profileFavorites", "") ?: "",
        p.getInt("profileAccent", 0)
    )
}

private fun saveProfile(context: Context, profile: ProfileSettings) {
    context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).edit()
        .putString("profileName", profile.displayName)
        .putString("profileBio", profile.bio)
        .putString("profileAvatar", profile.avatarUrl)
        .putInt("profileBanner", profile.bannerStyle)
        .putString("profileFavorites", profile.favoriteTitles)
        .putInt("profileAccent", profile.accentStyle)
        .apply()
}

@Composable
fun ProfileScreen(context: Context, library: List<Anime>,
    readingLibrary: List<ReadingTitle>, modifier: Modifier = Modifier) {
    var profile by remember { mutableStateOf(loadProfile(context)) }
    var editing by remember { mutableStateOf(false) }
    val bannerColors = listOf(
        listOf(Color(0xFF30294F), Color(0xFF171923), Color(0xFF252039)),
        listOf(Color(0xFF193D45), Color(0xFF171923), Color(0xFF24334A)),
        listOf(Color(0xFF4A283D), Color(0xFF171923), Color(0xFF34253F)),
        listOf(Color(0xFF343A45), Color(0xFF171923), Color(0xFF242B39))
    )
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(18.dp))
        Text("YOUR SPACE", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(bannerColors[profile.bannerStyle.coerceIn(0, 3)]),
            RoundedCornerShape(22.dp)).padding(18.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (profile.avatarUrl.isNotBlank()) {
                        AsyncImage(model = profile.avatarUrl, contentDescription = "Profile avatar",
                            modifier = Modifier.size(68.dp).background(Panel2, CircleShape))
                    } else {
                        Box(Modifier.size(68.dp).background(Panel2, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Violet, modifier = Modifier.size(34.dp))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(profile.displayName, color = White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("ANILARPER MEMBER", color = Violet, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    }
                }
                Text(profile.bio.ifBlank { "No bio yet. Make this space yours." }, color = White.copy(alpha = .85f), fontSize = 12.sp)
                OutlinedButton(onClick = { editing = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Violet.copy(alpha = .6f))) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Customize profile")
                }
            }
        }
        Text("YOUR COLLECTION", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ProfileStat("Anime", library.size.toString(), Modifier.weight(1f))
            ProfileStat("Completed", library.count { it.watchStatus == "COMPLETED" }.toString(), Modifier.weight(1f))
            ProfileStat("Reading", readingLibrary.size.toString(), Modifier.weight(1f))
        }
        Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("FAVORITES", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(profile.favoriteTitles.ifBlank { "Pin your favorite anime, manga, or light novels in Customize profile." },
                color = if (profile.favoriteTitles.isBlank()) Muted else White, fontSize = 13.sp)
        }
        Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(17.dp)).padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ACHIEVEMENT SHOWCASE", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            val achievements = listOf(
                Triple("Collector", "Save your first anime", library.isNotEmpty()),
                Triple("First finale", "Complete an anime", library.any { it.watchStatus == "COMPLETED" }),
                Triple("Binge tracker", "Log 10 episodes", library.sumOf { it.progress } >= 10),
                Triple("Bookworm", "Add a reading title", readingLibrary.isNotEmpty())
            )
            achievements.forEach { (name, description, unlocked) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(34.dp).background(if (unlocked) Violet.copy(alpha = .16f) else Panel2, CircleShape),
                        contentAlignment = Alignment.Center) {
                        Icon(if (unlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                            contentDescription = null, tint = if (unlocked) Violet else Muted, modifier = Modifier.size(17.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(name, color = if (unlocked) White else Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(description, color = Muted, fontSize = 10.sp)
                    }
                    Text(if (unlocked) "UNLOCKED" else "LOCKED",
                        color = if (unlocked) Violet else Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text("Profile settings are saved on this device.", color = Muted, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
    }
    if (editing) {
        var name by remember(profile) { mutableStateOf(profile.displayName) }
        var bio by remember(profile) { mutableStateOf(profile.bio) }
        var avatar by remember(profile) { mutableStateOf(profile.avatarUrl) }
        var favorites by remember(profile) { mutableStateOf(profile.favoriteTitles) }
        var banner by remember(profile) { mutableIntStateOf(profile.bannerStyle) }
        var accent by remember(profile) { mutableIntStateOf(profile.accentStyle) }
        AlertDialog(
            onDismissRequest = { editing = false }, containerColor = Panel2,
            title = { Text("Customize your profile", color = White, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Display name", color = Muted, fontSize = 11.sp)
                    OutlinedTextField(name, { name = it.take(32) }, singleLine = true,
                        placeholder = { Text("Your display name") })
                    Text("Bio", color = Muted, fontSize = 11.sp)
                    OutlinedTextField(bio, { bio = it.take(140) }, minLines = 2,
                        placeholder = { Text("A little about your anime taste") })
                    Text("Avatar image URL (optional)", color = Muted, fontSize = 11.sp)
                    OutlinedTextField(avatar, { avatar = it.take(500) }, singleLine = true,
                        placeholder = { Text("https://…") })
                    Text("Favorite titles", color = Muted, fontSize = 11.sp)
                    OutlinedTextField(favorites, { favorites = it.take(180) }, minLines = 2,
                        placeholder = { Text("Separate titles with commas") })
                    Text("Accent color", color = Muted, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccentPalette.forEachIndexed { index, color ->
                            Box(Modifier.weight(1f).height(34.dp)
                                .background(color, RoundedCornerShape(10.dp))
                                .clickable { accent = index }) {
                                if (accent == index) Icon(Icons.Default.Check, null, tint = Ink,
                                    modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    }
                    Text("Profile banner", color = Muted, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bannerColors.forEachIndexed { index, colors ->
                            Box(Modifier.weight(1f).height(38.dp)
                                .background(Brush.linearGradient(colors), RoundedCornerShape(9.dp))
                                .clickable { banner = index }) {
                                if (banner == index) Icon(Icons.Default.Check, null, tint = White,
                                    modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    profile = ProfileSettings(name.ifBlank { "Anime Explorer" }, bio, avatar, banner, favorites, accent)
                    VioletState.value = AccentPalette[accent.coerceIn(0, AccentPalette.lastIndex)]
                    saveProfile(context, profile)
                    editing = false
                }) { Text("Save profile", color = Violet) }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel", color = Muted) } }
        )
    }
}


@Composable
private fun ProfileStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(14.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Muted, fontSize = 10.sp)
    }
}
