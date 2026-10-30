package app.reportamelo.communityIssues.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reportamelo.commons.CommonIntercommunicationErrors
import app.reportamelo.commons.ReportViolationStatus
import app.reportamelo.commons.TypeOfContentToReport
import app.reportamelo.commons.utils.formatRelativeDate
import app.reportamelo.commons.utils.resolveUrlString
import app.reportamelo.models.Comment
import app.reportamelo.models.CommentToBlock
import app.reportamelo.models.ReportViolation
import app.reportamelo.repository.ModerationRepository
import app.reportamelo.repository.UserRepository
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class CommentViolationReportOptions(val id: String, val title: String) {
    HARASSMENT("harassment", "Harassment or bullying"),
    SELF_PROMOTION("selfPromotion", "Unwanted self-promotion"),
    HATE_SPEECH("hateSpeech", "Hate speech or discrimination"),
    VIOLENCE("violence", "Threats or violence"),
    OTHER("other", "Other issue")
}

private val SecondaryLabelColor = Color(0xFF8E8E93)

private fun userAlias(userName: String): String {
    if (userName.isBlank()) return ""
    return if (userName.startsWith("@")) userName else "@$userName"
}

private fun disableReportViolationButton(profileId: String): Boolean {
    return profileId == "currentUser" || UserRepository.shared.isOwnProfile(profileId)
}

@Composable
fun CommentRow(
    modifier: Modifier = Modifier,
    comment: Comment,
    hideReportOption: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf(CommentViolationReportOptions.OTHER.title) }
    val coroutineScope = rememberCoroutineScope()

    val resolvedAvatarUrl = remember(comment.profilePicture) {
        resolveUrlString(comment.profilePicture)
    }
    val relativeDate = remember(comment.createdAt) {
        formatRelativeDate(comment.createdAt)
    }
    val shouldDisableReport = remember(comment.profileId, hideReportOption) {
        hideReportOption || disableReportViolationButton(comment.profileId)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (resolvedAvatarUrl != null) {
                SubcomposeAsyncImage(
                    model = resolvedAvatarUrl,
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    error = {
                        CommentAvatarFallback(name = comment.name)
                    }
                )
            } else {
                CommentAvatarFallback(name = comment.name)
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = comment.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
                    )
                    if (comment.userName.isNotBlank()) {
                        Text(
                            text = userAlias(comment.userName),
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryLabelColor
                        )
                    }
                }

                Text(
                    text = relativeDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryLabelColor
                )
            }

            if (!shouldDisableReport) {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreHoriz,
                            contentDescription = "Comment report options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        Text(
                            text = "Comment report options",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryLabelColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        CommentViolationReportOptions.entries.forEachIndexed { index, option ->
                            DropdownMenuItem(
                                text = { Text(option.title) },
                                onClick = {
                                    selectedOption = option.title
                                    showMenu = false
                                    coroutineScope.launch {
                                        delay(128)
                                        showDialog = true
                                    }
                                }
                            )
                            if (index < CommentViolationReportOptions.entries.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = comment.message,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm content blocking") },
            text = { Text("I confirm that this content violates our community guidelines.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        coroutineScope.launch {
                            reportCommentViolation(comment, selectedOption)
                        }
                    }
                ) {
                    Text("Block", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CommentAvatarFallback(
    modifier: Modifier = Modifier,
    name: String
) {
    val initial = name.trim().firstOrNull()?.lowercaseChar()?.toString() ?: ""
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                if (initial.isNotEmpty()) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center
    ) {
        if (initial.isNotEmpty()) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

private suspend fun reportCommentViolation(comment: Comment, selectedOption: String) {
    val payload = CommentToBlock(
        id = comment.id ?: "",
        message = comment.message,
        profileId = comment.profileId,
        commentFor = comment.commentFor,
        resourceId = comment.resourceId
    )
    val blockedReasonId = CommentViolationReportOptions.entries
        .firstOrNull { it.title == selectedOption }?.id
        ?: CommentViolationReportOptions.OTHER.id
    val violation = ReportViolation(
        type = TypeOfContentToReport.COMMENT,
        content = payload,
        contentAuthorProfileId = comment.profileId,
        reason = selectedOption,
        blockedReasonId = blockedReasonId,
        status = ReportViolationStatus.SENT_TO_MODERATION
    )
    try {
        ModerationRepository.moderateComment(reason = violation, type = TypeOfContentToReport.COMMENT)
        println("moderation sent")
    } catch (_: CommonIntercommunicationErrors.ServerError) {
        println("Server Error")
    } catch (_: CommonIntercommunicationErrors.NetworkError) {
        println("It looks like that your network is experiencing some delays, please try again.")
    } catch (e: Exception) {
        println(e)
    }
}
