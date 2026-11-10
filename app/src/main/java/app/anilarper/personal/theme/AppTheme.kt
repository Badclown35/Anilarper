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

// Core palette. Names are unchanged so every existing screen keeps compiling and
// picks up the new look automatically; values are a deeper "midnight indigo" base.
internal val Ink = Color(0xFF090A14)
internal val Panel = Color(0xFF131528)
internal val Panel2 = Color(0xFF1C1F3A)
internal val AccentPalette = listOf(
    Color(0xFF9B8CFF), Color(0xFF5EDCF0), Color(0xFFFF8FBF), Color(0xFFFFC36E), Color(0xFF7DE3A6)
)
internal val VioletState = mutableStateOf(AccentPalette.first())
internal val Violet: Color get() = VioletState.value
internal val Muted = Color(0xFFA3A7C2)
internal val White = Color(0xFFF6F4FF)

// Secondary "anime pop" colors used for gradients, glows and highlights.
internal val Sakura = Color(0xFFFF8FB8)
internal val Aqua = Color(0xFF6FE3F5)
internal val Outline = Color(0xFF343862)

private val AnilarperTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 21.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = .3.sp)
)

private val AnilarperShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
internal fun AnilarperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Violet,
            onPrimary = Ink,
            primaryContainer = Violet.copy(alpha = .22f),
            onPrimaryContainer = White,
            secondary = Sakura,
            onSecondary = Ink,
            tertiary = Aqua,
            onTertiary = Ink,
            background = Ink,
            onBackground = White,
            surface = Panel,
            onSurface = White,
            surfaceVariant = Panel2,
            onSurfaceVariant = Muted,
            outline = Outline,
            outlineVariant = Panel2,
            surfaceContainerLowest = Ink,
            surfaceContainerLow = Panel,
            surfaceContainer = Panel,
            surfaceContainerHigh = Panel2,
            surfaceContainerHighest = Panel2
        ),
        typography = AnilarperTypography,
        shapes = AnilarperShapes,
        content = content
    )
}
