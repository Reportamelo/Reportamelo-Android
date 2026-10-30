package app.reportamelo.communityIssues.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import app.reportamelo.models.*

@Composable
fun PhotoPreview(
    url: String,
    width: Int = 170,
    height: Int = 170,
    isFullMode: Boolean = false,
    state: AttachmentState = AttachmentState.CONFIRMED,
    uploaderName: String = "User",
    date: String = "Today",
    onOptionsClick: () -> Unit = {}
) {
    val cornerRadius = 16.dp

    Box(
        modifier = Modifier
            .then(
                if (isFullMode) {
                    Modifier.fillMaxWidth().aspectRatio(3f / 4f)
                } else {
                    Modifier.size(width.dp, height.dp)
                }
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.Gray.copy(alpha = 0.2f))
    ) {
        AsyncImage(
            model = url,
            contentDescription = "Photo Preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (state == AttachmentState.PENDING) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.HourglassEmpty, 
                    contentDescription = "Pending", 
                    tint = Color.White, 
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Overlay text
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(8.dp)
        ) {
            Column {
                Text(uploaderName, style = MaterialTheme.typography.labelSmall, color = Color.White)
                Text(date, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
            }
        }

        // Options button
        IconButton(
            onClick = onOptionsClick,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Options", tint = Color.White)
        }
    }
}
