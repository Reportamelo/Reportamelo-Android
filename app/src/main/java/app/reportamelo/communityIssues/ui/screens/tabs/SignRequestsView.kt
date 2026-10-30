package app.reportamelo.communityIssues.ui.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignRequestsView() {
    var showCreateRequestView by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    val petitions = listOf(
        PetitionPost("Petition 1", "Desc 1", "cat1", 0.5f, 100, "50%"),
        PetitionPost("Petition 2", "Desc 2", "cat2", 0.2f, 200, "20%")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Petitions") },
                actions = {
                    IconButton(onClick = { showCreateRequestView = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                    IconButton(onClick = { /* Open menu for filters */ }) {
                        Icon(Icons.Default.List, contentDescription = "Options")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.12f))
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "${petitions.size} Petitions   100 signatories",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                itemsIndexed(petitions) { _, petition ->
                    PetitionCard(petition)
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PetitionCard(petition: PetitionPost) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = petition.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                Text(
                    text = petition.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text("Post Publisher Placeholder")
            Text("Petition View Post Placeholder")

            Divider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { petition.progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${petition.percentage} of ${petition.targetSignatures} signatures",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row {
                Text("Signatories Placeholder")
            }

            Divider()

            Text("Post Interactions Placeholder")
        }
    }
}
