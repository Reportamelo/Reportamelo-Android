package app.reportamelo.communityIssues.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ThemedButtonType {
    PRIMARY, SECONDARY, OUTLINE, DANGER
}

enum class ThemedButtonStyle {
    PROMINENT, NORMAL
}

@Composable
fun ThemedButton(
    modifier: Modifier = Modifier,
    message: String = "",
    onClick: () -> Unit,
    type: ThemedButtonType = ThemedButtonType.SECONDARY,
    style: ThemedButtonStyle = ThemedButtonStyle.PROMINENT,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    isEnabled: Boolean = true
) {
    val isDark = isSystemInDarkTheme()

    val containerColor = when (type) {
        ThemedButtonType.PRIMARY -> MaterialTheme.colorScheme.primary
        ThemedButtonType.SECONDARY -> MaterialTheme.colorScheme.secondary
        ThemedButtonType.OUTLINE -> if (isDark) Color.White.copy(alpha = 0.1f) else Color.White
        ThemedButtonType.DANGER -> MaterialTheme.colorScheme.error
    }

    val contentColor = when (type) {
        ThemedButtonType.PRIMARY -> if (isDark) Color(0xFF452602) else MaterialTheme.colorScheme.onBackground
        ThemedButtonType.SECONDARY -> Color.White
        ThemedButtonType.OUTLINE -> MaterialTheme.colorScheme.onBackground
        ThemedButtonType.DANGER -> Color.White
    }

    val disabledAlpha = when (type) {
        ThemedButtonType.PRIMARY, ThemedButtonType.SECONDARY -> 0.51f
        ThemedButtonType.OUTLINE -> 0.3f
        ThemedButtonType.DANGER -> 0.5f
    }

    val border = when (type) {
        ThemedButtonType.PRIMARY -> if (isEnabled) {
            BorderStroke(
                1.dp,
                lerp(MaterialTheme.colorScheme.primary, Color.White, if (isDark) 0.1f else 0.4f)
            )
        } else null
        ThemedButtonType.SECONDARY -> BorderStroke(
            1.dp,
            lerp(MaterialTheme.colorScheme.secondary, Color.Black, 0.01f)
        )
        ThemedButtonType.OUTLINE -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ThemedButtonType.DANGER -> null
    }

    val textStyle = when (type) {
        ThemedButtonType.PRIMARY -> if (style == ThemedButtonStyle.NORMAL) {
            MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        } else {
            MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        }
        ThemedButtonType.SECONDARY -> if (style == ThemedButtonStyle.NORMAL) {
            MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.3.sp)
        } else {
            MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
        }
        ThemedButtonType.OUTLINE -> if (style == ThemedButtonStyle.NORMAL) {
            MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Normal)
        } else {
            MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        }
        ThemedButtonType.DANGER -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
    }

    Button(
        onClick = {
            if (!isLoading) {
                onClick()
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = disabledAlpha),
            disabledContentColor = if (type == ThemedButtonType.SECONDARY) {
                Color.White.copy(alpha = 0.65f)
            } else {
                contentColor.copy(alpha = disabledAlpha)
            }
        ),
        border = border,
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp)
    ) {
        if (isLoading) {
            ShimmerEffect(modifier = Modifier.fillMaxSize())
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null)
                }
                Text(text = message, style = textStyle)
            }
        }
    }
}

@Composable
fun ShimmerEffect(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.1f),
            Color.White.copy(alpha = 0.4f),
            Color.White.copy(alpha = 0.1f),
        ),
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(brush)
    )
}
