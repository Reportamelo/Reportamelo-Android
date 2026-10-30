package app.reportamelo.communityIssues.ui.screens.animations

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

@Composable
fun ProgressRing(
    progress: Double,
    color: Color,
    size: Dp,
    lineWidth: Dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.toFloat(),
        animationSpec = tween(durationMillis = 1000),
        label = "ProgressRingAnimation"
    )

    Canvas(modifier = Modifier.size(size)) {
        val strokeWidth = lineWidth.toPx()
        
        // Background ring
        drawArc(
            color = color.copy(alpha = 0.2f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth)
        )
        
        // Foreground progress ring
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * animatedProgress,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}
