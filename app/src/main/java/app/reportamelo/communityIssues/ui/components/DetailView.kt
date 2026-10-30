package app.reportamelo.communityIssues.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.reportamelo.models.MapExplorerReport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailView(report: MapExplorerReport, onDismiss: () -> Unit) {
    val scrollState = rememberScrollState()
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = { },
                actions = {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Copy Report ID") }, onClick = { showMenu = false })
                        DropdownMenuItem(text = { Text("Report content") }, onClick = { showMenu = false })
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            DetailsHeader(title = report.title, description = report.description)
            
            BasicInformationView(report = report)
            
            EvidenceOfTheIssuesView(attachments = report.attachments, id = report.id)
            
            FollowUpSectionView(report = report, voteCount = 0)
            
            MoreInformationView(report = report)
            
            Column {
                Text(
                    text = "Latest Comments",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                // ForEach comment logic would go here
            }
        }
    }
}
