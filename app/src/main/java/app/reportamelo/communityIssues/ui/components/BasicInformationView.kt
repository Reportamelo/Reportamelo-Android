package app.reportamelo.communityIssues.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reportamelo.commons.IssueStatus
import app.reportamelo.commons.IssueTypes
import app.reportamelo.commons.Severity
import app.reportamelo.communityIssues.R
import app.reportamelo.models.MapExplorerReport
import app.reportamelo.communityIssues.ui.theme.Dimensions

@Composable
fun BasicInformationView(
    modifier: Modifier = Modifier,
    report: MapExplorerReport
) {
    val issueTypeLabel = when (IssueTypes.fromIdentifier(report.issueTypeId)) {
        IssueTypes.ROAD -> stringResource(R.string.road)
        IssueTypes.PUBLIC_SPACE -> stringResource(R.string.public_space)
        IssueTypes.BUILDING -> stringResource(R.string.building)
        IssueTypes.OTHER -> stringResource(R.string.other)
        IssueTypes.ALL -> stringResource(R.string.all)
    }

    val severityLabel = when (Severity.fromIdentifier(report.severityId)) {
        Severity.LOW -> stringResource(R.string.low)
        Severity.MEDIUM -> stringResource(R.string.medium)
        Severity.HIGH -> stringResource(R.string.high)
        Severity.ALL -> stringResource(R.string.all)
    }

    val statusLabel = when (IssueStatus.fromIdentifier(report.statusId)) {
        IssueStatus.REPORTED -> stringResource(R.string.reported)
        IssueStatus.CONFIRMED -> stringResource(R.string.confirmed)
        IssueStatus.IN_PROGRESS -> stringResource(R.string.in_progress)
        IssueStatus.PETITION_TO_SIGN -> stringResource(R.string.petition_to_sign)
        IssueStatus.FIXED -> stringResource(R.string.fixed)
        IssueStatus.ASSIGNED -> stringResource(R.string.assigned)
        IssueStatus.REJECTED -> stringResource(R.string.rejected)
        IssueStatus.RECTIFICATION -> stringResource(R.string.rectification)
        IssueStatus.HUMAN_REVIEW -> stringResource(R.string.human_review)
        IssueStatus.UNDER_REVIEW -> stringResource(R.string.under_review)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        InfoBox(
            modifier = Modifier.weight(1f),
            label = "Issue Type",
            value = issueTypeLabel
        )
        InfoBox(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.severity),
            value = severityLabel
        )
        InfoBox(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.status),
            value = statusLabel
        )
    }
}

@Composable
private fun InfoBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        Color(0xFFF2F2F7)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(Dimensions.ThemeRadius * 1.1f))
            .background(bgColor)
            .padding(horizontal = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
