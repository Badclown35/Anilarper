package app.anilarper.personal

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.anilarper.personal.theme.*
import app.anilarper.personal.data.AnimeLibraryViewModel
import app.anilarper.personal.data.ReadingLibraryViewModel
import app.anilarper.personal.data.CollectionViewModelFactory
import app.anilarper.personal.data.DiscoverViewModel
import app.anilarper.personal.ui.home.HomeScreen
import app.anilarper.personal.ui.reading.ReadingScreen
import app.anilarper.personal.ui.library.LibraryScreen
import app.anilarper.personal.ui.discover.DiscoverScreen
import app.anilarper.personal.ui.music.MusicScreen
import app.anilarper.personal.ui.quests.QuestScreen
import app.anilarper.personal.ui.backup.BackupScreen
import app.anilarper.personal.ui.stats.WatchHistoryScreen
import app.anilarper.personal.ui.stats.StatisticsScreen
import app.anilarper.personal.ui.profile.ProfileScreen
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AnilarperApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnilarperApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: "Home"
    var moreOpen by remember { mutableStateOf(false) }
    val activity = context as ComponentActivity
    val collectionFactory = remember(context) { CollectionViewModelFactory(context.applicationContext) }
    val animeViewModel = remember(activity) {
        ViewModelProvider(activity, collectionFactory)[AnimeLibraryViewModel::class.java]
    }
    val readingViewModel = remember(activity) {
        ViewModelProvider(activity, collectionFactory)[ReadingLibraryViewModel::class.java]
    }
    val discoverViewModel = remember(activity) {
        ViewModelProvider(activity)[DiscoverViewModel::class.java]
    }
    val library by animeViewModel.library.collectAsState()
    val readingLibrary by readingViewModel.library.collectAsState()
    LaunchedEffect(Unit) {
        val accent = context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).getInt("profileAccent", 0)
        VioletState.value = AccentPalette[accent.coerceIn(0, AccentPalette.lastIndex)]
    }
    AnilarperTheme {
        Scaffold(
            containerColor = Ink,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF11131B), tonalElevation = 0.dp) {
                    listOf("Home" to Icons.Default.Home, "Discover" to Icons.Default.Explore,
                        "Library" to Icons.Default.Bookmarks, "More" to Icons.Default.MoreHoriz).forEach { (label, icon) ->
                        val isSelected = if (label == "More")
                            currentRoute in setOf("Read", "Music", "Quests", "Profile", "Stats", "Backup", "History")
                        else currentRoute == label
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (label == "More") moreOpen = true
                                else navController.navigate(label) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label,
                                modifier = Modifier.size(if (isSelected) 23.dp else 21.dp)) },
                            label = { Text(label, fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, maxLines = 1) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Violet, selectedTextColor = Violet,
                                indicatorColor = Violet.copy(alpha = .14f),
                                unselectedIconColor = Color(0xFF777B8D),
                                unselectedTextColor = Color(0xFF777B8D)
                            )
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(navController = navController, startDestination = "Home", modifier = Modifier.padding(padding)) {
                composable("Home") {
                    HomeScreen(Modifier.fillMaxSize(), library,
                        onDiscover = { navController.navigate("Discover") { launchSingleTop = true } },
                        onOpenLibrary = { navController.navigate("Library") { launchSingleTop = true } },
                        onOpenRead = { navController.navigate("Read") { launchSingleTop = true } },
                        onOpenQuests = { navController.navigate("Quests") { launchSingleTop = true } },
                        onOpenStats = { navController.navigate("Stats") { launchSingleTop = true } },
                        onOpenProfile = { navController.navigate("Profile") { launchSingleTop = true } })
                }
                composable("Discover") {
                    DiscoverScreen(
                        Modifier.fillMaxSize(), library.map { it.id }.toSet(), discoverViewModel
                    ) { anime -> animeViewModel.toggle(anime) }
                }
                composable("Library") {
                    LibraryScreen(Modifier.fillMaxSize(), library,
                        onRemove = { animeViewModel.remove(it) },
                        onUpdate = { animeViewModel.update(it) })
                }
                composable("Read") {
                    ReadingScreen(Modifier.fillMaxSize(), readingLibrary,
                        onAdd = { readingViewModel.add(it) },
                        onUpdate = { readingViewModel.update(it) },
                        onRemove = { readingViewModel.remove(it) })
                }
                composable("Music") { MusicScreen(Modifier.fillMaxSize()) }
                composable("Quests") { QuestScreen(context, library, readingLibrary, Modifier.fillMaxSize()) }
                composable("Stats") { StatisticsScreen(library, readingLibrary, Modifier.fillMaxSize()) }
                composable("History") { WatchHistoryScreen(library, Modifier.fillMaxSize()) }
                composable("Backup") {
                    BackupScreen(context, Modifier.fillMaxSize(), onRestored = {
                        animeViewModel.refresh()
                        readingViewModel.refresh()
                        val accent = context.getSharedPreferences("anilarper", Context.MODE_PRIVATE)
                            .getInt("profileAccent", 0)
                        VioletState.value = AccentPalette[accent.coerceIn(0, AccentPalette.lastIndex)]
                    })
                }
                composable("Profile") { ProfileScreen(context, library, readingLibrary, Modifier.fillMaxSize()) }
            }
        }
        if (moreOpen) {
            ModalBottomSheet(onDismissRequest = { moreOpen = false }, containerColor = Panel) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MORE FROM ANILARPER", color = Violet, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    listOf(
                        Triple("Read", "Manga & light novels", Icons.Default.MenuBook),
                        Triple("Music", "Your soundtrack sources", Icons.Default.LibraryMusic),
                        Triple("Quests", "Milestones & XP", Icons.Default.EmojiEvents),
                        Triple("Stats", "Your anime and reading activity", Icons.Default.Insights),
                        Triple("History", "Recently updated episodes", Icons.Default.History),
                        Triple("Backup", "Export or restore your collection", Icons.Default.Backup),
                        Triple("Profile", "Your personal space", Icons.Default.Person)
                    ).forEach { (label, subtitle, icon) ->
                        Row(Modifier.fillMaxWidth().clickable { navController.navigate(label) { launchSingleTop = true }; moreOpen = false }
                            .background(Panel2, RoundedCornerShape(14.dp)).padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                            Icon(icon, contentDescription = null, tint = Violet)
                            Column(Modifier.weight(1f)) {
                                Text(label, color = White, fontWeight = FontWeight.SemiBold)
                                Text(subtitle, color = Muted, fontSize = 11.sp)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Muted)
                        }
                    }
                }
            }
        }
    }
}


