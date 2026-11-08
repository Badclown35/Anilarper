package app.anilarper.personal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF0C0D12)
private val Panel = Color(0xFF171923)
private val Panel2 = Color(0xFF202331)
private val Violet = Color(0xFF9B8CFF)
private val Muted = Color(0xFF9A9EAE)
private val White = Color(0xFFF4F3FA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AnilarperApp() }
    }
}

@Composable
private fun AnilarperApp() {
    var selected by remember { mutableStateOf("Home") }
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Ink, surface = Panel, primary = Violet,
            onBackground = White, onSurface = White
        )
    ) {
        Scaffold(
            containerColor = Ink,
            bottomBar = {
                NavigationBar(containerColor = Panel, contentColor = White) {
                    val tabs = listOf(
                        "Home" to Icons.Default.Home,
                        "Discover" to Icons.Default.Explore,
                        "Library" to Icons.Default.Bookmarks,
                        "Quests" to Icons.Default.EmojiEvents,
                        "Profile" to Icons.Default.Person
                    )
                    tabs.forEach { (label, icon) ->
                        NavigationBarItem(
                            selected = selected == label,
                            onClick = { selected = label },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Violet,
                                selectedTextColor = Violet,
                                indicatorColor = Violet.copy(alpha = .14f),
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            when (selected) {
                "Home" -> HomeScreen(Modifier.padding(padding))
                "Discover" -> ModuleScreen("Discover", "Find your next favorite series.", Modifier.padding(padding))
                "Library" -> ModuleScreen("Your library", "Your anime and manga collection will live here.", Modifier.padding(padding))
                "Quests" -> ModuleScreen("Nexus quests", "Progress at your own pace. No streak pressure.", Modifier.padding(padding))
                else -> ModuleScreen("Your profile", "Your anime identity, stats, and collection.", Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun HomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ANILARPER", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Your next chapter.", color = White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.size(42.dp).background(Panel2, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = "Profile", tint = Violet)
            }
        }

        Box(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(Color(0xFF282345), Color(0xFF171923), Color(0xFF252039))),
                RoundedCornerShape(24.dp)
            ).padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("YOUR COMEBACK STARTS HERE", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Text("Find the feeling\nthat got you into anime.", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 29.sp)
                Text("A personal space for anime, manga, and discoveries.", color = Muted, fontSize = 13.sp)
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Start exploring", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        SectionTitle("Your space", "Personalized for you")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTile("Anime", "Track series", Icons.Default.Tv, Modifier.weight(1f))
            QuickTile("Manga", "Track chapters", Icons.Default.MenuBook, Modifier.weight(1f))
            QuickTile("Quests", "Earn at your pace", Icons.Default.EmojiEvents, Modifier.weight(1f))
        }

        SectionTitle("A little inspiration", "No pressure. Just discovery.")
        Column(
            Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("TODAY'S DISCOVERY", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text("Your recommendations will appear here.", color = White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("Once the anime catalog is connected, Anilarper can find picks based on your real watch history and ratings.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }

        SectionTitle("Coming together", "The first build")
        Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).padding(16.dp)) {
            FeatureLine("Personal anime + manga library")
            FeatureLine("AniList-powered search and details")
            FeatureLine("Revival recommendations")
            FeatureLine("XP, quests, and achievements")
            FeatureLine("MAL list import")
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun QuickTile(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Panel, RoundedCornerShape(16.dp)).clickable { }.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Violet, modifier = Modifier.size(22.dp))
        Text(title, color = White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(subtitle, color = Muted, fontSize = 10.sp, lineHeight = 13.sp)
    }
}

@Composable
private fun FeatureLine(text: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Violet, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = White, fontSize = 13.sp)
    }
}

@Composable
private fun ModuleScreen(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.height(12.dp))
        Text(title, color = White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 14.sp)
        Text("This module is part of the full Anilarper build.", color = Violet, fontSize = 12.sp)
    }
}
