package app.reportamelo.communityIssues.ui.screens.modals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// Mocks
data class PreviewAttachment(val id: String, val url: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvidencesScreen(
    reportId: String,
    reportContainer: String,
    onDismiss: () -> Unit,
    onNavigateToPhotoDetail: (String) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var evidences by remember { mutableStateOf<List<PreviewAttachment>>(emptyList()) }
    var isAttachingNewEvidences by remember { mutableStateOf(false) }

    LaunchedEffect(reportId) {
        isLoading = true
        delay(1000) // Mock network delay
        // evidences = listOf(PreviewAttachment("1", "url"), PreviewAttachment("2", "url"))
        evidences = emptyList() // Start empty to show empty state
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Evidences") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    if (evidences.size < 24) {
                        IconButton(onClick = { isAttachingNewEvidences = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Evidence")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                evidences.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "No evidences found.",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "There are no evidences attached to this report yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(evidences) { photo ->
                            Card(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clickable { onNavigateToPhotoDetail(photo.id) }
                            ) {
                                // Mocking the photo image loading
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Photo")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isAttachingNewEvidences) {
        AttachEvidencesScreen(
            onDismiss = { isAttachingNewEvidences = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachEvidencesScreen(onDismiss: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Evidences") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = { onDismiss() },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text("Attach new evidence")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            Text("Attach new evidence", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Image Picker Placeholder")
        }
    }
}
