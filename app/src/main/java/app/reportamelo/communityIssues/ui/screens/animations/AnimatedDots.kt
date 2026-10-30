package app.reportamelo.communityIssues.ui.screens.animations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.delay

@Composable
fun AnimatedDots(
    animate: Boolean = true,
    content: @Composable () -> Unit
) {
    var dotCount by remember { mutableIntStateOf(1) }

    LaunchedEffect(animate) {
        if (animate) {
            while (true) {
                delay(450)
                dotCount = (dotCount % 4) + 1
            }
        }
    }

    Row {
        content()
        Box(contentAlignment = Alignment.CenterStart) {
            Text(text = "....", modifier = Modifier.alpha(0f))
            Text(text = ".".repeat(dotCount))
        }
    }
}
