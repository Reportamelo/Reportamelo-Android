package app.reportamelo.communityIssues.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkPrimaryForeground,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBackground,
    surface = DarkCardBackground,
    onSurface = DarkCardForeground,
    onBackground = DarkForeground,
    error = DarkDestructive,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    surfaceVariant = DarkMuted,
    // Containers used by Material components (sheets, menus, dialogs, cards);
    // without these they fall back to the purple-tinted Material baseline
    surfaceContainerLowest = DarkCardBackground,
    surfaceContainerLow = DarkBackground,
    surfaceContainer = DarkCardBackground,
    surfaceContainerHigh = DarkAccent,
    surfaceContainerHighest = DarkInputBackground
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightPrimaryForeground,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackground,
    surface = LightCardBackground,
    onSurface = LightCardForeground,
    onBackground = LightForeground,
    error = LightDestructive,
    outline = LightBorder,
    outlineVariant = LightBorder,
    surfaceVariant = LightMuted,
    // Containers used by Material components (sheets, menus, dialogs, cards);
    // without these they fall back to the purple-tinted Material baseline
    surfaceContainerLowest = LightCardBackground,
    surfaceContainerLow = LightBackground,
    surfaceContainer = LightCardBackground,
    surfaceContainerHigh = LightAccent,
    surfaceContainerHighest = LightInputBackground
)

@Composable
fun CommunityIssueReporterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Fill for the round toolbar buttons (close, back, more): same tone as the cards
// so they sit on the view background instead of the warm muted color
@Composable
fun toolbarButtonColor(): Color = if (isSystemInDarkTheme()) {
    MaterialTheme.colorScheme.surfaceVariant
} else {
    MaterialTheme.colorScheme.surface
}

@Composable
fun toolbarButtonBorder(): BorderStroke = BorderStroke(
    0.5.dp,
    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
)

@Composable
fun Modifier.toolbarButtonBackground(shape: Shape = CircleShape): Modifier =
    this
        .background(toolbarButtonColor(), shape)
        .border(toolbarButtonBorder(), shape)

@Composable
fun Modifier.radialGradientBackground(
    topLeading: Boolean = true,
    radiusFraction: Float = 0.54f
): Modifier {
    val isDark = isSystemInDarkTheme()
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val backgroundColor = MaterialTheme.colorScheme.background
    val startColor = lerp(
        secondaryColor,
        if (isDark) Color.Black else Color.White,
        0.4f
    ).copy(alpha = 0.67f)

    return this.drawBehind {
        drawRect(color = backgroundColor)
        val radius = (size.width * radiusFraction).coerceAtLeast(1f)
        val center = if (topLeading) Offset(0f, 0f) else Offset(size.width, 0f)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(startColor, backgroundColor),
                center = center,
                radius = radius
            )
        )
    }
}
