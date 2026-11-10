package app.anilarper.personal

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import app.anilarper.personal.data.ReadingSearchViewModel
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
import app.anilarper.personal.ui.extras.CustomListsScreen
import app.anilarper.personal.ui.extras.RecommendationsScreen
import app.anilarper.personal.ui.extras.MalImportScreen
import app.anilarper.personal.ui.extras.SeasonalScheduleScreen
import app.anilarper.personal.ui.extras.LocalVideoPlayerScreen
import app.anilarper.personal.ui.extras.CommunityVideosScreen
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
    val readingSearchViewModel = remember(activity) {
        ViewModelProvider(activity)[ReadingSearchViewModel::class.java]
    }
    val library by animeViewModel.library.collectAsState()
    val readingLibrary by readingViewModel.library.collectAsState()
    LaunchedEffect(Unit) {
        val accent = context.getSharedPreferences("anilarper", Context.MODE_PRIVATE).getInt("profileAccent", 0)
        VioletState.value = AccentPalette[accent.coerceIn(0, AccentPalette.lastIndex)]
    }
    AnilarperTheme {
        AnimeBackdrop {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(
                    containerColor = Panel.copy(alpha = .96f),
                    tonalElevation = 0.dp,
                    modifier = Modifier.drawBehind {
                        drawLine(
                            Brush.horizontalGradient(listOf(Color.Transparent, Violet.copy(alpha = .7f), Sakura.copy(alpha = .6f), Color.Transparent)),
                            Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 2f
                        )
                    }
                ) {
                    listOf("Home" to Icons.Default.Home, "Discover" to Icons.Default.Explore,
                        "Library" to Icons.Default.Bookmarks, "Read" to Icons.Default.MenuBook,
                        "More" to Icons.Default.MoreHoriz).forEach { (label, icon) ->
                        val isSelected = if (label == "More")
                            currentRoute in setOf("More", "Read", "Music", "Quests", "Profile", "Stats", "Backup", "History", "CustomLists", "Recommendations", "MALImport", "Schedule", "VideoPlayer", "CommunityVideos")
                        else currentRoute == label
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(label) {
                                    // Always navigate to the explicitly selected top-level destination.
                                    // Restoring saved stacks here was causing the bottom bar to reopen
                                    // an unrelated More destination (for example Music or Read).
                                    popUpTo("Home") { inclusive = false; saveState = false }
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label,
                                modifier = Modifier.size(if (isSelected) 23.dp else 21.dp)) },
                            label = { Text(label, fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, maxLines = 1) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Violet, selectedTextColor = Violet,
                                indicatorColor = Violet.copy(alpha = .22f),
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted
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
                composable("More") { MoreScreen { route -> navController.navigate(route) { launchSingleTop = true } } }
                composable("CustomLists") { CustomListsScreen(library, Modifier.fillMaxSize()) }
                composable("Recommendations") { RecommendationsScreen(library, { animeViewModel.toggle(it) }, Modifier.fillMaxSize()) }
                composable("MALImport") { MalImportScreen({ animeViewModel.importAnime(it) }, Modifier.fillMaxSize()) }
                composable("Schedule") { SeasonalScheduleScreen({ animeViewModel.toggle(it) }, library.map { it.id }.toSet(), Modifier.fillMaxSize()) }
                composable("VideoPlayer") { LocalVideoPlayerScreen(Modifier.fillMaxSize()) }
                composable("CommunityVideos") { CommunityVideosScreen(Modifier.fillMaxSize()) }
                composable("Library") {
                    LibraryScreen(Modifier.fillMaxSize(), library,
                        onRemove = { animeViewModel.remove(it) },
                        onUpdate = { animeViewModel.update(it) })
                }
                composable("Read") {
                    ReadingScreen(Modifier.fillMaxSize(), readingLibrary, readingSearchViewModel,
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
        }
    }
}

@Composable
private fun MoreScreen(onNavigate: (String) -> Unit) {
    val entries = listOf(
        Triple("Read", "Manga & light novels", Icons.Default.MenuBook),
        Triple("Music", "Soundtracks and local tracks", Icons.Default.LibraryMusic),
        Triple("Quests", "Milestones & XP", Icons.Default.EmojiEvents),
        Triple("Stats", "Anime and reading activity", Icons.Default.Insights),
        Triple("History", "Recently updated episodes", Icons.Default.History),
        Triple("Backup", "Export or restore your collection", Icons.Default.Backup),
        Triple("Profile", "Personalize your profile", Icons.Default.Person),
        Triple("CustomLists", "Create and manage anime collections", Icons.Default.Bookmarks),
        Triple("Recommendations", "Discover anime based on your library", Icons.Default.AutoAwesome),
        Triple("MALImport", "Import your MyAnimeList anime CSV", Icons.Default.FileDownload),
        Triple("Schedule", "Browse anime by season and year", Icons.Default.CalendarMonth),
        Triple("VideoPlayer", "Play video files from your device", Icons.Default.VideoLibrary),
        Triple("CommunityVideos", "Browse videos shared from the community server", Icons.Default.Cloud)
    )
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("MORE", color = Violet, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
        Text("Your tools & extras", color = White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text("Everything beyond your main library.", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        entries.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { (route, subtitle, icon) ->
                    MoreTile(route, subtitle, icon, Modifier.weight(1f)) { onNavigate(route) }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MoreTile(route: String, subtitle: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val label = when (route) {
        "MALImport" -> "MAL Import"
        "CustomLists" -> "Custom lists"
        "VideoPlayer" -> "Video player"
        "CommunityVideos" -> "Community videos"
        else -> route
    }
    Column(
        modifier.height(138.dp).animeCard(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            Modifier.size(42.dp).background(
                Brush.linearGradient(listOf(Violet.copy(alpha = .35f), Sakura.copy(alpha = .2f))), RoundedCornerShape(14.dp)
            ),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = Violet) }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            Text(subtitle, color = Muted, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 3)
        }
    }
}
