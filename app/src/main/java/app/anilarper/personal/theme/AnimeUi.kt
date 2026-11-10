package app.anilarper.personal.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/** Default rounded shape for cards across the app. */
internal val CardShape = RoundedCornerShape(20.dp)

/** Gradient accent used for bars, borders and glows (follows the user's accent color). */
@Composable
internal fun accentBrush(): Brush = Brush.horizontalGradient(listOf(Violet, Sakura))

/**
 * Anime-style card surface: soft indigo gradient with a thin violet-to-sakura rim light.
 * Apply BEFORE `.clickable {}` so the ripple stays inside the rounded corners.
 */
@Composable
internal fun Modifier.animeCard(shape: Shape = CardShape): Modifier =
    this.clip(shape)
        .background(Brush.linearGradient(listOf(Panel2, Panel)), shape)
        .border(
            1.dp,
            Brush.linearGradient(listOf(Violet.copy(alpha = .5f), Color.Transparent, Sakura.copy(alpha = .3f))),
            shape
        )

/** Full-screen midnight backdrop with soft violet and sakura glows. */
@Composable
internal fun AnimeBackdrop(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Ink).drawBehind {
            drawRect(
                Brush.radialGradient(
                    listOf(Violet.copy(alpha = .22f), Color.Transparent),
                    center = Offset(size.width * .95f, 0f), radius = size.width * .95f
                )
            )
            drawRect(
                Brush.radialGradient(
                    listOf(Sakura.copy(alpha = .11f), Color.Transparent),
                    center = Offset(0f, size.height * .6f), radius = size.width
                )
            )
        }
    ) { content() }
}

/** Section header with a gradient accent bar and an optional action button. */
@Composable
internal fun SectionTitle(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.width(4.dp).height(if (subtitle != null) 34.dp else 22.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(Violet, Sakura)))
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            if (subtitle != null) Text(subtitle, color = Muted, fontSize = 11.sp)
        }
        if (actionLabel != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = Violet, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Small capsule label, e.g. "EP 5 / 12" or "NOW WATCHING". */
@Composable
internal fun Pill(text: String, modifier: Modifier = Modifier, color: Color = Violet) {
    Text(
        text,
        modifier = modifier
            .background(color.copy(alpha = .16f), CircleShape)
            .border(1.dp, color.copy(alpha = .45f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp, maxLines = 1
    )
}

/** Cropped, rounded cover image (fixes stretched / letterboxed posters). */
@Composable
internal fun PosterImage(url: String, contentDescription: String?, modifier: Modifier = Modifier, corner: Dp = 14.dp) {
    AsyncImage(
        model = url, contentDescription = contentDescription, contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(corner)).background(Panel2)
    )
}

/** Thin gradient progress bar. */
@Composable
internal fun GradientProgress(fraction: Float, modifier: Modifier = Modifier) {
    Box(modifier.height(6.dp).clip(CircleShape).background(Ink.copy(alpha = .6f))) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(CircleShape).background(Brush.horizontalGradient(listOf(Violet, Sakura)))
        )
    }
}
