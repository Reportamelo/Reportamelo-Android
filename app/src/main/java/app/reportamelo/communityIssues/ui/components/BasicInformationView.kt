package app.reportamelo.communityIssues.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*


import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.reportamelo.models.MapExplorerReport
import app.reportamelo.communityIssues.ui.theme.Dimensions

@Composable
fun BasicInformationView(report: MapExplorerReport) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        InfoBox(
            modifier = Modifier.weight(1f),
            label = "Issue Type",
            value = report.id
        )
        InfoBox(
            modifier = Modifier.weight(1f),
            label = "Severity",
            value = report.statusId.toString()
        )
        InfoBox(
            modifier = Modifier.weight(1f),
            label = "Status",
            value = report.statusId.toString()
        )
    }
}

@Composable
private fun InfoBox(modifier: Modifier = Modifier, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(Dimensions.ThemeRadius * 1.1f))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}
