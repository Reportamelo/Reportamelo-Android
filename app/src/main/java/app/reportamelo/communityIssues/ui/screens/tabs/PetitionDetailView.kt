package app.reportamelo.communityIssues.ui.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class PetitionPost(
    val title: String,
    val description: String,
    val categoryId: String,
    val progress: Float,
    val targetSignatures: Int,
    val percentage: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetitionDetailView(petition: PetitionPost, offline: Boolean = false) {
    val scrollState = rememberScrollState()
    var showSignatureModal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(petition.title) },
                actions = {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { showSignatureModal = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Sign")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("Image Carousel Placeholder", modifier = Modifier.align(Alignment.Center))
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("Publisher info placeholder")

                SectionHeader("Description")
                Text(
                    text = petition.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SectionHeader("Category")
                Text(petition.categoryId)

                SectionHeader("Location")
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Intersection of Main St and Elm St", fontWeight = FontWeight.SemiBold)
                        Text("123 Main St, Anytown, USA", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Latitude: 99", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Longitude: 99", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                SectionHeader("Severity")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null)
                    Text("High", fontWeight = FontWeight.Medium)
                }

                SectionHeader("Confirmation Status")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Confirmed by 3 users", fontWeight = FontWeight.Medium)
                }

                SectionHeader("Petition")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { petition.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("${petition.percentage} of ${petition.targetSignatures} signatures", style = MaterialTheme.typography.bodySmall)
                }

                SectionHeader("Comments")
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth()
    )
}
