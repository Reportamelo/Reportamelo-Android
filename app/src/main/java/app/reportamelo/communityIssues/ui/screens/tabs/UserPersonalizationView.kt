package app.reportamelo.communityIssues.ui.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPersonalizationView(
    onNextStep: () -> Unit
) {
    var userName by remember { mutableStateOf("") }
    val name by remember { mutableStateOf("John Doe") }
    var isUserNameValid by remember { mutableStateOf(false) }
    val isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personalize your profile") }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = onNextStep,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isUserNameValid && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Next Step")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Text("Profile Image Placeholder")
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text("@$userName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = userName,
                    onValueChange = { 
                        userName = it
                        isUserNameValid = it.length in 3..20
                    },
                    label = { Text("User Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = !isUserNameValid && userName.isNotEmpty(),
                    supportingText = {
                        if (!isUserNameValid && userName.isNotEmpty()) {
                            Text("User name must be between 3 and 20 characters")
                        } else if (isUserNameValid) {
                            Text("Available", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        }
    }
}
