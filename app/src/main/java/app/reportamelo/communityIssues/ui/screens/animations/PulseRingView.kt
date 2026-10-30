package app.reportamelo.communityIssues.ui.screens.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
fun PulseRingView(
    tint: Color,
    size: Dp
) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        for (i in 0 until 3) {
            PulseRing(tint = tint, delayMillis = i * 200)
        }
    }
}

@Composable
private fun PulseRing(tint: Color, delayMillis: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseRingTransition")
    
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, delayMillis = delayMillis, easing = EaseInOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseRingProgress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val scale = animationProgress * 2f
        val alpha = if (animationProgress == 0f) 0.4f else (1f - animationProgress) * 0.4f
        val radius = (size.minDimension / 2) * scale

        if (alpha > 0f && radius > 0f) {
            drawCircle(
                color = tint.copy(alpha = alpha),
                radius = radius
            )
        }
    }
}
