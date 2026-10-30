package app.reportamelo.communityIssues.ui.screens.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.reportamelo.commons.ViewOptions
import app.reportamelo.communityIssues.controllers.MyCommentsViewModel
import app.reportamelo.communityIssues.ui.components.CommentRow
import app.reportamelo.communityIssues.ui.components.LoadingView
import app.reportamelo.communityIssues.ui.theme.LightCardBackground
import app.reportamelo.communityIssues.ui.theme.radialGradientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSubView(
    modifier: Modifier = Modifier,
    subViewName: String = "My Comments",
    mode: ViewOptions = ViewOptions.LIST,
    viewModel: MyCommentsViewModel = viewModel(),
    onBack: (() -> Unit)? = null
) {
    val comments by viewModel.comments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val isDark = isSystemInDarkTheme()
    val cardBackground = if (isDark) MaterialTheme.colorScheme.surface else LightCardBackground

    LaunchedEffect(Unit) {
        viewModel.fetchComments(page = 1)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .radialGradientBackground(topLeading = true, radiusFraction = 0.54f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top bar matching iOS NavigationBar with circular back button and centered title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (onBack != null) {
                    Surface(
                        modifier = Modifier
                            .size(42.dp)
                            .align(Alignment.CenterStart),
                        shape = CircleShape,
                        color = cardBackground,
                        tonalElevation = 0.dp,
                        shadowElevation = 2.dp,
                        border = BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        ),
                        onClick = onBack
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Text(
                    text = subViewName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }

            // Content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLoading && comments.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingView()
                    }
                } else if (comments.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Forum,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(34.dp)
                                    .align(Alignment.TopStart),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No comments yet.",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Please tell us how that problem affects you.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8E8E93),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = comments,
                            key = { it.id ?: it.hashCode() }
                        ) { comment ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        comment.id?.let { id ->
                                            viewModel.deleteComment(id)
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = true,
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(MaterialTheme.colorScheme.error)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onError
                                        )
                                    }
                                },
                                content = {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(24.dp),
                                        color = cardBackground,
                                        tonalElevation = 0.dp,
                                        shadowElevation = 2.dp,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Box(modifier = Modifier.padding(16.dp)) {
                                            CommentRow(
                                                comment = comment,
                                                hideReportOption = true
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
