package app.reportamelo.communityIssues.ui.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Notifications(var app: Boolean, var email: Boolean, var web: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EssentialInformationView(
    notifications: Notifications,
    onNotificationsChanged: (Notifications) -> Unit,
    finalStep: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Notes") })
        },
        bottomBar = {
            Box(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                Button(
                    onClick = finalStep,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start reporting")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).padding(16.dp)
        ) {
            Text("Notifications", style = MaterialTheme.typography.titleLarge)
            Text("Please give me permission to send you notifications about status of your reports and follow up.\nyou can change these settings in the app settings.", style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Push notifications")
                Switch(
                    checked = notifications.app,
                    onCheckedChange = { onNotificationsChanged(notifications.copy(app = it)) }
                )
            }
            
            Divider()
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Email notifications")
                Switch(
                    checked = notifications.email,
                    onCheckedChange = { onNotificationsChanged(notifications.copy(email = it)) }
                )
            }
        }
    }
}
