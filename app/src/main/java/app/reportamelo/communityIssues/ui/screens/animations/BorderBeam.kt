package app.reportamelo.communityIssues.ui.screens.animations

import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.borderBeam(
    border: Color,
    hideFadeBorder: Boolean = true,
    beam: List<Color>,
    cornerRadius: Dp,
    isEnabled: Boolean = true
): Modifier = composed {
    if (!isEnabled) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "BorderBeamTransition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BorderBeamRotation"
    )

    this.drawWithContent {
        drawContent()

        val cornerRadiusPx = CornerRadius(cornerRadius.toPx())

        if (!hideFadeBorder) {
            drawRoundRect(
                color = border.copy(alpha = 0.3f),
                cornerRadius = cornerRadiusPx,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        val borderGradient = Brush.sweepGradient(
            colors = listOf(Color.Transparent, border, Color.Transparent),
        )
        
        rotate(degrees = rotation) {
            drawRoundRect(
                brush = borderGradient,
                cornerRadius = cornerRadiusPx,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
