package app.reportamelo.communityIssues.ui.screens.modals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Mocks
data class Report(
    val id: String,
    val description: String,
    val address: String,
    val reportedAt: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsChooserScreen(
    reports: List<Report>,
    initialSelectedReports: List<String>,
    onSelectionChanged: (List<String>) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var selectedReports by remember { mutableStateOf(initialSelectedReports.toSet()) }

    val filteredReports = if (searchText.isEmpty()) {
        reports
    } else {
        reports.filter { it.description.contains(searchText, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search a report") },
            singleLine = true
        )

        if (filteredReports.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("No reports yet.", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Please send us reports.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredReports, key = { it.id }) { report ->
                    val isSelected = selectedReports.contains(report.id)
                    ReportSelectionRow(
                        report = report,
                        isSelected = isSelected,
                        onClick = {
                            val newSelection = if (isSelected) {
                                selectedReports - report.id
                            } else {
                                selectedReports + report.id
                            }
                            selectedReports = newSelection
                            onSelectionChanged(newSelection.toList())
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun ReportSelectionRow(
    report: Report,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = report.description,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = report.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (report.reportedAt != null) {
                Text(
                    text = "Reported at: ${report.reportedAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
