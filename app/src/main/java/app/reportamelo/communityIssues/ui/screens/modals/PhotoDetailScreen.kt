package app.reportamelo.communityIssues.ui.screens.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(
    photos: List<PreviewAttachment>,
    initialPhotoId: String,
    onDismiss: () -> Unit
) {
    val initialIndex = remember {
        photos.indexOfFirst { it.id == initialPhotoId }.takeIf { it >= 0 } ?: 0
    }
    
    val pagerState = rememberPagerState(initialPage = initialIndex) { photos.size }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val photo = photos[page]
            AsyncImage(
                model = photo.url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight(0.8f)
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit
            )
        }

        // Custom Toolbar Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "${pagerState.currentPage + 1} / ${photos.size}",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.weight(1f))
            
            // Dummy box to keep alignment centered
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}
