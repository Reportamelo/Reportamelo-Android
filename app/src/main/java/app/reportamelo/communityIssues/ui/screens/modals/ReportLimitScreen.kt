package app.reportamelo.communityIssues.ui.screens.modals

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.reportamelo.communityIssues.controllers.InteractionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportLimitScreen(
    modifier: Modifier = Modifier,
    isGuestUser: Boolean,
    onDismiss: () -> Unit,
    continuationHandler: (InteractionType) -> Unit,
    onShowPaywall: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!isGuestUser) {
                    Button(
                        onClick = onShowPaywall,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Subscribe")
                    }
                }

                OutlinedButton(
                    onClick = {
                        isLoading = true
                        // Mock ad logic
                        continuationHandler(InteractionType.VIEW_AD)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("View an Ad")
                    }
                }

                OutlinedButton(
                    onClick = {
                        continuationHandler(InteractionType.NO_THANKS)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("No, Thanks")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "You have reached your free limit!",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Thank you for using our App, you have reached the free limit for this month but you can choose to see an ad or pay a monthly subscription.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
