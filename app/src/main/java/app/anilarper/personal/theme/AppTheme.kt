package app.anilarper.personal.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

internal val Ink = Color(0xFF0C0D12)
internal val Panel = Color(0xFF171923)
internal val Panel2 = Color(0xFF202331)
internal val AccentPalette = listOf(
    Color(0xFF9B8CFF), Color(0xFF72C8D8), Color(0xFFFF9FBD), Color(0xFFFFC36E), Color(0xFF91CFA5)
)
internal val VioletState = mutableStateOf(AccentPalette.first())
internal val Violet: Color get() = VioletState.value
internal val Muted = Color(0xFF9A9EAE)
internal val White = Color(0xFFF4F3FA)

@Composable
internal fun AnilarperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Ink,
            surface = Panel,
            primary = Violet,
            onBackground = White,
            onSurface = White
        ),
        content = content
    )
}
