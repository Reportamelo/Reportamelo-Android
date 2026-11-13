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
import androidx.compose.ui.res.stringResource
import app.reportamelo.communityIssues.R

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
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
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
                        Text(stringResource(R.string.subscribe))
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
                        Text(stringResource(R.string.view_an_ad))
                    }
                }

                OutlinedButton(
                    onClick = {
                        continuationHandler(InteractionType.NO_THANKS)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(stringResource(R.string.no_thanks))
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
                text = stringResource(R.string.you_have_reached_your_free_limit),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = stringResource(R.string.thank_you_for_using_our_app_you_have_reached_the_free_limit_for_this_month_but_you_can_choose_to_see_an_ad_or_pay_a_monthly_subscription),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
