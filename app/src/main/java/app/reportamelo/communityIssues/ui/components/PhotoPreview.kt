package app.reportamelo.communityIssues.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reportamelo.commons.ReportAttachmentState
import app.reportamelo.commons.ReportViolationStatus
import app.reportamelo.commons.TypeOfContentToReport
import app.reportamelo.communityIssues.R
import app.reportamelo.models.*
import app.reportamelo.repository.ModerationRepository
import app.reportamelo.repository.UserRepository
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ContentViolationReportOptions(val id: String, val description: String) {
    INAPPROPRIATE_CONTENT("inappropriateContent", "Inappropriate or explicit content"),
    SPAM("spam", "Spam"),
    VIOLENCE("violence", "Violence"),
    EXPLICIT_CONTENT("explicitContent", "Explicit content"),
    ANIMAL_ABUSE("animalAbuse", "Animal abuse"),
    OTHER("other", "Other")
}

private fun formatAttachmentDate(epochRaw: Long): String {
    val millis = if (epochRaw < 100_000_000_000L) epochRaw * 1000L else epochRaw
    val formatter = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}

@Composable
fun PhotoPreview(
    modifier: Modifier = Modifier,
    attachment: PreviewAttachment,
    height: Dp = 160.dp,
    width: Dp = 160.dp,
    isFullMode: Boolean = false,
    showOptions: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val isGuest = remember { UserRepository.shared.isGuestUser() }
    var showMenu by remember { mutableStateOf(false) }
    var selectedReason by remember { mutableStateOf<ContentViolationReportOptions?>(null) }
    var showConfirmAlert by remember { mutableStateOf(false) }
    var showResultAlert by remember { mutableStateOf(false) }
    var alertTitle by remember { mutableStateOf("") }
    var alertMessage by remember { mutableStateOf("") }

    val formattedDate = remember(attachment.createdAtRaw) {
        formatAttachmentDate(attachment.createdAtRaw)
    }
    val displayUser = remember(attachment.uploaderUserName) {
        val raw = attachment.uploaderUserName.ifEmpty { "guest" }
        if (raw.startsWith("@")) raw else "@$raw"
    }

    Box(modifier = modifier) {
        PhotoPreview(
            modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            url = attachment.url?.toString() ?: "",
            width = width.value.toInt(),
            height = height.value.toInt(),
            isFullMode = isFullMode,
            state = attachment.state,
            uploaderName = displayUser,
            date = formattedDate,
            showOptionsButton = showOptions && !isGuest,
            onOptionsClick = { showMenu = true }
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            ContentViolationReportOptions.entries.forEach { option ->
                val label = when (option) {
                    ContentViolationReportOptions.INAPPROPRIATE_CONTENT -> stringResource(R.string.inappropriate_or_explicit_content)
                    ContentViolationReportOptions.SPAM -> stringResource(R.string.spam)
                    ContentViolationReportOptions.VIOLENCE -> stringResource(R.string.violence)
                    ContentViolationReportOptions.EXPLICIT_CONTENT -> stringResource(R.string.explicit_content)
                    ContentViolationReportOptions.ANIMAL_ABUSE -> stringResource(R.string.animal_abuse)
                    ContentViolationReportOptions.OTHER -> stringResource(R.string.other)
                }
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        showMenu = false
                        selectedReason = option
                        showConfirmAlert = true
                    }
                )
            }
        }
    }

    if (showConfirmAlert && selectedReason != null) {
        val receivedTitle = stringResource(R.string.we_ve_received_your_report)
        val receivedMsg = stringResource(R.string.meanwhile_you_can_copy_report_code_or_share_it_with_others)
        val errorTitle = stringResource(R.string.something_went_wrong)
        val errorMsg = stringResource(R.string.an_unexpected_error_has_occurred_please_try_again)

        AlertDialog(
            onDismissRequest = { showConfirmAlert = false },
            title = { Text(stringResource(R.string.confirm_content_blocking)) },
            text = { Text(stringResource(R.string.i_confirm_that_this_content_violates_our_community_guidelines)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val reason = selectedReason ?: return@TextButton
                        showConfirmAlert = false
                        coroutineScope.launch {
                            try {
                                val violation = ReportViolation(
                                    type = TypeOfContentToReport.IMAGE,
                                    content = attachment,
                                    contentAuthorProfileId = attachment.uploaderUserName,
                                    reason = reason.description,
                                    blockedReasonId = reason.id,
                                    status = ReportViolationStatus.PENDING
                                )
                                ModerationRepository.moderateContent(
                                    reason = violation,
                                    type = TypeOfContentToReport.IMAGE
                                )
                                alertTitle = receivedTitle
                                alertMessage = receivedMsg
                                showResultAlert = true
                            } catch (e: Exception) {
                                alertTitle = errorTitle
                                alertMessage = errorMsg
                                showResultAlert = true
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.yes), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmAlert = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showResultAlert) {
        AlertDialog(
            onDismissRequest = { showResultAlert = false },
            title = { Text(alertTitle) },
            text = { Text(alertMessage) },
            confirmButton = {
                TextButton(onClick = { showResultAlert = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun PhotoPreview(
    modifier: Modifier = Modifier,
    url: String,
    width: Int = 170,
    height: Int = 170,
    isFullMode: Boolean = false,
    state: AttachmentState = AttachmentState.CONFIRMED,
    uploaderName: String = "User",
    date: String = "Today",
    showOptionsButton: Boolean = true,
    onOptionsClick: () -> Unit = {}
) {
    val cornerRadius = 22.dp
    val isBlurred = state != ReportAttachmentState.CONFIRMED

    Box(
        modifier = modifier
            .then(
                if (isFullMode) {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
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
            modifier = Modifier
                .fillMaxSize()
                .then(if (isBlurred) Modifier.blur(15.dp) else Modifier)
        )

        if (isBlurred) {
            val statusIcon = when (state) {
                ReportAttachmentState.PENDING -> Icons.Filled.HourglassEmpty
                ReportAttachmentState.INAPPROPRIATE -> Icons.Filled.VisibilityOff
                ReportAttachmentState.DELETED -> Icons.Filled.Delete
                ReportAttachmentState.MANUAL_REVISION -> Icons.Filled.ManageSearch
                ReportAttachmentState.CONFIRMED -> Icons.Filled.CheckCircle
            }
            val statusText = when (state) {
                ReportAttachmentState.PENDING -> stringResource(R.string.pending)
                ReportAttachmentState.INAPPROPRIATE -> stringResource(R.string.inappropriate_or_explicit_content)
                ReportAttachmentState.DELETED -> stringResource(R.string.removed)
                ReportAttachmentState.MANUAL_REVISION -> stringResource(R.string.human_review)
                ReportAttachmentState.CONFIRMED -> stringResource(R.string.confirmed)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = statusText,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Bottom gradient overlay matching iOS Screenshot 1 & 11
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.72f)
                        )
                    )
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text = uploaderName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = date,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1
                )
            }
        }

        // Top-right circular "..." button matching iOS Screenshot 1 & 11
        if (showOptionsButton) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.75f))
                    .border(0.5.dp, Color.Black.copy(alpha = 0.12f), CircleShape)
                    .clickable(onClick = onOptionsClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreHoriz,
                    contentDescription = "Options",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
