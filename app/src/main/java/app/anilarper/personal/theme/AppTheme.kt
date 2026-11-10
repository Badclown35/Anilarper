package app.anilarper.personal.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Shared midnight palette; existing screens keep their behavior and pick up the refresh.
internal val Ink = Color(0xFF080B12)
internal val Panel = Color(0xFF111722)
internal val Panel2 = Color(0xFF1A2533)
internal val AccentPalette = listOf(
    Color(0xFF9B8CFF), Color(0xFF55D7E8), Color(0xFFFF8FBF),
    Color(0xFFFFC36E), Color(0xFF7DE3A6)
)
internal val VioletState = mutableStateOf(AccentPalette.first())
internal val Violet: Color get() = VioletState.value
internal val Muted = Color(0xFF9BA8B8)
internal val White = Color(0xFFF4F7FB)
internal val Sakura = Color(0xFFFF8FB8)
internal val Aqua = Color(0xFF68DCEB)
internal val Outline = Color(0xFF2A394B)

private val AnilarperTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 25.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 21.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = .2.sp)
)
private val AnilarperShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(17.dp), large = RoundedCornerShape(23.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
internal fun AnilarperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Violet, onPrimary = Ink,
            primaryContainer = Violet.copy(alpha = .2f), onPrimaryContainer = White,
            secondary = Sakura, onSecondary = Ink, tertiary = Aqua, onTertiary = Ink,
            background = Ink, onBackground = White, surface = Panel, onSurface = White,
            surfaceVariant = Panel2, onSurfaceVariant = Muted, outline = Outline,
            outlineVariant = Panel2, surfaceContainerLowest = Ink,
            surfaceContainerLow = Panel, surfaceContainer = Panel,
            surfaceContainerHigh = Panel2, surfaceContainerHighest = Panel2
        ),
        typography = AnilarperTypography,
        shapes = AnilarperShapes,
        content = content
    )
}
