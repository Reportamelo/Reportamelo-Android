package app.reportamelo.communityIssues.helpers

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import app.reportamelo.communityIssues.ui.theme.Dimensions

fun Modifier.cardStyle(color: Color): Modifier {
    val shape = RoundedCornerShape(Dimensions.ThemeRadius * 1.4f)
    val startColor = lerp(color, Color.Black, 0.4f).copy(alpha = 0.96f)
    val endColor = color.copy(alpha = 0f)
    return this
        .clip(shape)
        .drawBehind {
            val radius = (size.width * 0.8f).coerceAtLeast(1f)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(startColor, endColor),
                    center = Offset(0f, size.height),
                    radius = radius
                )
            )
        }
}
