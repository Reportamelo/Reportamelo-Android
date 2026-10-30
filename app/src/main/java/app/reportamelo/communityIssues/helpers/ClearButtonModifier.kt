package app.reportamelo.communityIssues.helpers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.clearButton(
    text: String,
    onClear: () -> Unit,
    isPill: Boolean = false,
    disabled: Boolean = false
): Modifier {
    // In Android Compose, trailing icons are preferred over overlays in modifiers
    // This provides equivalent padding adjustment for a clear button
    return this.padding(end = if (text.isEmpty()) 0.dp else 8.dp)
}

@Composable
fun ClearButtonOverlay(
    text: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    isPill: Boolean = false,
    disabled: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.padding(end = if (text.isEmpty()) 0.dp else 8.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        content()
        if (text.isNotEmpty() && !disabled) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear",
                tint = Color.Gray,
                modifier = Modifier
                    .padding(end = if (isPill) 16.dp else 0.dp)
                    .clickable { onClear() }
            )
        }
    }
}
