package app.reportamelo.communityIssues.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.reportamelo.communityIssues.R
import app.reportamelo.communityIssues.ui.screens.attachments.AttachmentDetailView
import app.reportamelo.models.Attachment
import app.reportamelo.models.IssueUpdate
import app.reportamelo.models.Milestone
import app.reportamelo.models.Resolution
import app.reportamelo.repository.ReportRepository

private val TimelineOrange = Color(0xFFF57C00)
private val FinalEvidencePurple = Color(0xFF7C1DFF)

@Composable
fun IssueTimeLineView(
    modifier: Modifier = Modifier,
    reportId: String,
    preloadedResolution: Resolution? = null,
    onBack: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null
) {
    var resolution by remember { mutableStateOf(preloadedResolution) }
    var isLoading by remember { mutableStateOf(preloadedResolution == null) }
    var activeAttachments by remember { mutableStateOf<Pair<String, List<Attachment>>?>(null) }

    LaunchedEffect(reportId, preloadedResolution) {
        if (preloadedResolution != null) {
            resolution = preloadedResolution
            isLoading = false
        } else {
            isLoading = true
            try {
                resolution = ReportRepository.fetchResolutionByReport(reportId)
            } catch (_: Exception) {
                resolution = null
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Navigation bar (< and X buttons matching Screenshot 3)
        if (onBack != null || onClose != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(42.dp))
                }

                if (onClose != null) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Large header + ID subtitle
        Text(
            text = stringResource(R.string.report_follow_up),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.id_s,
                resolution?.id?.takeIf { it.isNotBlank() } ?: stringResource(R.string.no_id_yet)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TimelineOrange)
            }
        } else {
            val history = resolution?.history

            // 1. Reported
            val reportedDate = history?.reported?.date ?: stringResource(R.string.pending)
            val reportedSubtitle = if (history?.reported != null) {
                stringResource(R.string.by_a_citizen)
            } else {
                stringResource(R.string.waiting_for_initial_report)
            }
            TimelineMilestoneSection(
                title = stringResource(R.string.reported),
                date = reportedDate,
                subtitle = reportedSubtitle,
                isLast = false
            )

            // 2. Confirmed
            val confirmedDate = history?.confirmed?.date ?: stringResource(R.string.pending)
            val confirmedSubtitle = history?.confirmed?.by?.takeIf { it.isNotBlank() }?.let {
                stringResource(R.string.report_confirmed_by_s, it)
            } ?: stringResource(R.string.waiting_for_confirmation)
            TimelineMilestoneSection(
                title = stringResource(R.string.confirmed),
                date = confirmedDate,
                subtitle = confirmedSubtitle,
                isLast = false
            )

            // 3. In Progress
            val updates = history?.inProgress?.updates.orEmpty()
            TimelineMilestoneSection(
                title = stringResource(R.string.in_progress),
                date = "",
                subtitle = if (updates.isEmpty()) stringResource(R.string.no_updates_yet) else null,
                isLast = false
            ) {
                if (updates.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        updates.forEach { update ->
                            InProgressUpdateRow(
                                update = update,
                                onViewAttachments = {
                                    activeAttachments = contextTitleForInProgress() to update.attachments
                                }
                            )
                        }
                    }
                }
            }

            // 4. Fixed
            val fixedDate = history?.fixed?.date ?: stringResource(R.string.pending)
            val fixedSubtitle = history?.fixed?.comments?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.pending_resolution)
            val fixedAttachments = history?.fixed?.attachments.orEmpty()
            val fixedTitle = stringResource(R.string.fixed)

            TimelineMilestoneSection(
                title = fixedTitle,
                date = fixedDate,
                subtitle = fixedSubtitle,
                isLast = true
            ) {
                if (fixedAttachments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { activeAttachments = fixedTitle to fixedAttachments },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FinalEvidencePurple,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.view_final_evidence),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    activeAttachments?.let { (sheetTitle, attachmentsList) ->
        Dialog(
            onDismissRequest = { activeAttachments = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            AttachmentDetailView(
                attachments = attachmentsList,
                title = sheetTitle,
                onDismiss = { activeAttachments = null }
            )
        }
    }
}

private fun contextTitleForInProgress(): String = "In Progress"

@Composable
private fun TimelineMilestoneSection(
    modifier: Modifier = Modifier,
    title: String,
    date: String,
    subtitle: String?,
    isLast: Boolean,
    extraContent: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        // Left column: Orange ring node + vertical connector line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(TimelineOrange),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .defaultMinSize(minHeight = 48.dp)
                        .background(TimelineOrange.copy(alpha = 0.45f))
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Right column: Milestone details
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 16.dp else 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (date.isNotBlank()) {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            extraContent()
        }
    }
}

@Composable
private fun InProgressUpdateRow(
    modifier: Modifier = Modifier,
    update: IssueUpdate,
    onViewAttachments: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(7.dp)
                .clip(CircleShape)
                .background(TimelineOrange)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = update.comments,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = update.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (update.attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TimelineOrange.copy(alpha = 0.12f))
                        .clickable { onViewAttachments() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = TimelineOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.view_evidence_d, update.attachments.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = TimelineOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun IssueTimeLineView(
    modifier: Modifier = Modifier,
    reportId: String,
    milestones: List<Milestone>
) {
    IssueTimeLineView(
        modifier = modifier,
        reportId = reportId
    )
}
