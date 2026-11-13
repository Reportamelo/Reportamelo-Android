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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
        // Mirrors iOS: the shimmer runs behind the label, so the text stays visible while loading
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                ShimmerEffect(
                    modifier = Modifier.fillMaxSize(),
                    shimmerColor = if (type == ThemedButtonType.OUTLINE) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    } else {
                        Color.White.copy(alpha = 0.35f)
                    }
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                if (icon != null && !isLoading) {
                    Icon(icon, contentDescription = null)
                }
                Text(
                    text = message,
                    style = textStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    shimmerColor: Color = Color.White.copy(alpha = 0.35f)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_phase"
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .drawBehind {
                // A band 1.5x the button width sweeping from leading to trailing
                val bandWidth = size.width * 1.5f
                val startX = -size.width * 0.75f + bandWidth * phase
                drawRect(
                    brush = Brush.horizontalGradient(
                        0.3f to Color.Transparent,
                        0.5f to shimmerColor,
                        0.7f to Color.Transparent,
                        startX = startX,
                        endX = startX + bandWidth
                    )
                )
            }
    )
}
