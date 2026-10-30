package app.reportamelo.communityIssues.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.reportamelo.communityIssues.controllers.ImagePhase
import coil.compose.AsyncImage

@Composable
fun PreviewImageToUpload(
    modifier: Modifier = Modifier,
    name: String,
    phase: ImagePhase,
    bitmap: Bitmap? = null,
    url: String? = null,
    aspectRatio: Float = 1f,
    currentValue: Float,
    total: Float = 1f,
    onDelete: (String) -> Unit,
    onRetry: (String) -> Unit
) {
    var showDeleteAlert by remember { mutableStateOf(false) }
    val completed = currentValue >= total
    val safeAspectRatio = aspectRatio.coerceIn(0.55f, 1.8f)

    if (showDeleteAlert) {
        AlertDialog(
            onDismissRequest = { showDeleteAlert = false },
            title = { Text("Delete") },
            text = { Text("Do you want to delete this image?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAlert = false
                        onDelete(name)
                    }
                ) {
                    Text("Yes", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAlert = false }) {
                    Text("No")
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(safeAspectRatio)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.TopEnd
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (!url.isNullOrEmpty()) {
            AsyncImage(
                model = url,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Bottom dark gradient overlay with phase label and progress bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.78f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                ),
            contentAlignment = Alignment.BottomStart
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = phase.description,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                if (!completed && phase != ImagePhase.SUCCESS) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { if (total > 0f) (currentValue / total).coerceIn(0f, 1f) else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.35f)
                    )
                }
            }
        }

        // Top-right action button: Trash (red) or Retry (orange/white) on failure
        if (phase != ImagePhase.FAILURE) {
            IconButton(
                onClick = { showDeleteAlert = true },
                enabled = phase != ImagePhase.DELETING,
                modifier = Modifier
                    .padding(8.dp)
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.85f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFFF3B30),
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            IconButton(
                onClick = { onRetry(name) },
                modifier = Modifier
                    .padding(8.dp)
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Retry",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
