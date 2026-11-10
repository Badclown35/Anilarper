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

internal val CardShape = RoundedCornerShape(20.dp)
@Composable
internal fun accentBrush(): Brush = Brush.horizontalGradient(listOf(Violet, Sakura))

/** Quiet, high-contrast card surface shared by all app sections. */
@Composable
internal fun Modifier.animeCard(shape: Shape = CardShape): Modifier =
    this.clip(shape)
        .background(Brush.linearGradient(listOf(Panel2.copy(alpha = .72f), Panel.copy(alpha = .98f))), shape)
        .border(1.dp, Outline.copy(alpha = .72f), shape)

/** Restrained midnight backdrop with subtle accent light. */
@Composable
internal fun AnimeBackdrop(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Ink).drawBehind {
            drawRect(Brush.radialGradient(
                colors = listOf(Violet.copy(alpha = .13f), Color.Transparent),
                center = Offset(size.width * .96f, 0f), radius = size.width * .9f
            ))
            drawRect(Brush.radialGradient(
                colors = listOf(Aqua.copy(alpha = .055f), Color.Transparent),
                center = Offset(0f, size.height * .72f), radius = size.width * .85f
            ))
        }
    ) { content() }
}

@Composable
internal fun SectionTitle(
    title: String, subtitle: String? = null, actionLabel: String? = null,
    onAction: () -> Unit = {}, modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(if (subtitle != null) 34.dp else 22.dp)
            .clip(CircleShape).background(Brush.verticalGradient(listOf(Violet, Sakura))))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, color = Muted, fontSize = 11.sp)
        }
        if (actionLabel != null) TextButton(onClick = onAction) {
            Text(actionLabel, color = Violet, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun Pill(text: String, modifier: Modifier = Modifier, color: Color = Violet) {
    Text(text, modifier = modifier.background(color.copy(alpha = .14f), CircleShape)
        .border(1.dp, color.copy(alpha = .38f), CircleShape)
        .padding(horizontal = 9.dp, vertical = 4.dp),
        color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold,
        letterSpacing = .7.sp, maxLines = 1)
}

@Composable
internal fun PosterImage(url: String, contentDescription: String?, modifier: Modifier = Modifier, corner: Dp = 14.dp) {
    AsyncImage(model = url, contentDescription = contentDescription, contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(corner)).background(Panel2))
}

@Composable
internal fun GradientProgress(fraction: Float, modifier: Modifier = Modifier) {
    Box(modifier.height(6.dp).clip(CircleShape).background(Outline.copy(alpha = .7f))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f))
            .clip(CircleShape).background(Brush.horizontalGradient(listOf(Violet, Sakura))))
    }
}
