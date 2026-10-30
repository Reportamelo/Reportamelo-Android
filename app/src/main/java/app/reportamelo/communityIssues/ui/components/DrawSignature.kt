package app.reportamelo.communityIssues.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.util.UUID

data class SignatureLine(
    val id: UUID = UUID.randomUUID(),
    val points: MutableList<Offset> = mutableListOf()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawSignature(
    strokes: MutableList<SignatureLine>,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit
) {
    var currentStroke by remember { mutableStateOf(SignatureLine()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Draw Signature") },
                navigationIcon = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                },
                actions = {
                    TextButton(onClick = onSave) {
                        Text("Save")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Create, contentDescription = "Draw")
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear signature")
                }
            }
        }
    ) { padding ->
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentStroke = SignatureLine(points = mutableListOf(offset))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentStroke.points.add(change.position)
                        },
                        onDragEnd = {
                            if (currentStroke.points.isNotEmpty()) {
                                strokes.add(currentStroke)
                                currentStroke = SignatureLine()
                            }
                        }
                    )
                }
        ) {
            val strokeStyle = Stroke(
                width = 3f * density,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )

            // Draw all completed strokes
            for (stroke in strokes) {
                if (stroke.points.size > 1) {
                    val path = Path().apply {
                        moveTo(stroke.points.first().x, stroke.points.first().y)
                        for (i in 1 until stroke.points.size) {
                            lineTo(stroke.points[i].x, stroke.points[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color.Black,
                        style = strokeStyle
                    )
                }
            }

            // Draw current stroke
            if (currentStroke.points.size > 1) {
                val path = Path().apply {
                    moveTo(currentStroke.points.first().x, currentStroke.points.first().y)
                    for (i in 1 until currentStroke.points.size) {
                        lineTo(currentStroke.points[i].x, currentStroke.points[i].y)
                    }
                }
                drawPath(
                    path = path,
                    color = Color.Black,
                    style = strokeStyle
                )
            }
        }
    }
}
