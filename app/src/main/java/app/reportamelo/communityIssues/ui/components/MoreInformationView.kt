package app.reportamelo.communityIssues.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.reportamelo.models.MapExplorerReport
import kotlin.math.abs

@Composable
fun MoreInformationView(report: MapExplorerReport) {
    val opacity = 0.85f
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "More Information", style = MaterialTheme.typography.titleMedium)
        
        InfoRow(label = "Report Id:", value = report.id)
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "Reported by:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = opacity),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "@${report.userName}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { /* navigate to profile */ }
            )
        }
        
        InfoRow(label = "Created on:", value = "")
        InfoRow(label = "Last update:", value = "")
        InfoRow(label = "Address:", value = report.address, maxLines = 2)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* open coordinates map */ },
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Coordinates:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = opacity),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${report.lat}° N, ${abs(report.lng)}° W",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, maxLines: Int = 1) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = maxLines
        )
    }
}
