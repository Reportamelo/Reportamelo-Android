package app.reportamelo.communityIssues.ui.screens.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

fun Modifier.skeleton(isRedacted: Boolean): Modifier = composed {
    if (!isRedacted) return@composed this

    val transition = rememberInfiniteTransition(label = "SkeletonTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SkeletonTranslate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f)
        ),
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    this.background(brush)
}
