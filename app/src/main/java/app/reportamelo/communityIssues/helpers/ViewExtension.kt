package app.reportamelo.communityIssues.helpers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.optionalGlassEffect(isDarkTheme: Boolean, cornerRadius: Dp = 30.dp): Modifier {
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    return this
        .background(
            color = backgroundColor.copy(alpha = 0.75f),
            shape = RoundedCornerShape(cornerRadius)
        )
        .clip(RoundedCornerShape(cornerRadius))
}

fun Modifier.optionalGlassWithShape(isDarkTheme: Boolean, shape: Shape): Modifier {
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    return this
        .background(
            color = backgroundColor.copy(alpha = 0.45f),
            shape = shape
        )
        .clip(shape)
}

fun Modifier.customStroke(color: Color, width: Dp = 1.dp): Modifier {
    return this
        .border(width = width, color = color)
        .padding(width)
}
