package app.reportamelo.communityIssues.ui.screens.animations

import android.graphics.BlurMaskFilter
import android.graphics.Matrix
import androidx.compose.animation.core.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/// Same arc as the iOS AngularGradient (140° → 270°)
private const val BEAM_START_ANGLE = 140f
private const val BEAM_SWEEP = 130f / 360f

private fun beamShader(center: Offset, colors: List<Color>): android.graphics.Shader {
    val stops = List(colors.size) { index -> BEAM_SWEEP * (index + 1) / (colors.size + 1) }
    return SweepGradientShader(
        center = center,
        colors = listOf(Color.Transparent) + colors + listOf(Color.Transparent, Color.Transparent),
        colorStops = listOf(0f) + stops + listOf(BEAM_SWEEP, 1f)
    )
}

fun Modifier.borderBeam(
    border: Color,
    hideFadeBorder: Boolean = true,
    beam: List<Color> = emptyList(),
    beamBlur: Dp = 16.dp,
    cornerRadius: Dp,
    isEnabled: Boolean = true
): Modifier = composed {
    if (!isEnabled) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "BorderBeamTransition")
    val rotation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BorderBeamRotation"
    )

    this.drawWithCache {
        val strokeWidth = 2.dp.toPx()
        val inset = strokeWidth / 2
        val radiusPx = cornerRadius.toPx()
        val strokeTopLeft = Offset(inset, inset)
        val strokeSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val strokeRadius = CornerRadius((radiusPx - inset).coerceAtLeast(0f))
        val center = Offset(size.width / 2, size.height / 2)
        val matrix = Matrix()

        /// Only the shader matrix rotates, the rounded rect stays in place so the gradient travels along the border
        val borderShader = beamShader(center, listOf(border))
        val borderBrush = ShaderBrush(borderShader)

        /// Beam Gradient, blurred and clipped to the card so it only glows inwards
        val beamShader = if (beam.isNotEmpty()) beamShader(center, beam) else null
        val blurPx = beamBlur.toPx()
        val beamPaint = beamShader?.let { shader ->
            Paint().apply {
                style = PaintingStyle.Stroke
                this.strokeWidth = blurPx
                this.shader = shader
                if (blurPx > 0f) {
                    asFrameworkPaint().maskFilter = BlurMaskFilter(blurPx / 1.5f, BlurMaskFilter.Blur.NORMAL)
                }
            }
        }
        val cardPath = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(radiusPx)))
        }

        onDrawWithContent {
            drawContent()

            /// Reading the animation here keeps it in the draw phase, no recomposition per frame
            matrix.setRotate(BEAM_START_ANGLE + rotation.value, center.x, center.y)
            borderShader.setLocalMatrix(matrix)

            if (!hideFadeBorder) {
                drawRoundRect(
                    color = border.copy(alpha = 0.3f),
                    topLeft = strokeTopLeft,
                    size = strokeSize,
                    cornerRadius = strokeRadius,
                    style = Stroke(width = strokeWidth)
                )
            }

            if (beamShader != null && beamPaint != null) {
                beamShader.setLocalMatrix(matrix)
                clipPath(cardPath) {
                    drawIntoCanvas { canvas ->
                        canvas.drawRoundRect(0f, 0f, size.width, size.height, radiusPx, radiusPx, beamPaint)
                    }
                }
            }

            /// Border Gradient
            drawRoundRect(
                brush = borderBrush,
                topLeft = strokeTopLeft,
                size = strokeSize,
                cornerRadius = strokeRadius,
                style = Stroke(width = strokeWidth)
            )
        }
    }
}
