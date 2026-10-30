package app.reportamelo.communityIssues.ui.screens.modals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.unit.dp

// Mocks
enum class PetitionCategory(val title: String, val minSignatures: Int) {
    ALL("Select a category", 0),
    ENVIRONMENT("Environment", 50),
    INFRASTRUCTURE("Infrastructure", 100)
}

data class Petition(
    var title: String = "",
    var description: String = "",
    var category: PetitionCategory = PetitionCategory.ALL,
    var targetSignatures: Int = 0,
    var reportsIds: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRequestPetitionScreen(
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    var petition by remember { mutableStateOf(Petition()) }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Petition") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isSubmitting = true
                            onSubmit()
                        },
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = "Submit")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Details of the request", style = MaterialTheme.typography.titleMedium)
            
            OutlinedTextField(
                value = petition.title,
                onValueChange = { petition = petition.copy(title = it) },
                label = { Text("Title") },
                placeholder = { Text("Please enter a title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = petition.description,
                onValueChange = { petition = petition.copy(description = it) },
                label = { Text("Description") },
                placeholder = { Text("Please enter a description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = petition.category.title,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    PetitionCategory.values().forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption.title) },
                            onClick = {
                                petition = petition.copy(
                                    category = selectionOption,
                                    targetSignatures = selectionOption.minSignatures
                                )
                                expanded = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider()
            
            Text("Reports", style = MaterialTheme.typography.titleMedium)
            
            ListItem(
                headlineContent = { Text("Choose report(s)") },
                trailingContent = {
                    if (petition.reportsIds.isNotEmpty()) {
                        Badge { Text(petition.reportsIds.size.toString()) }
                    } else {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                },
                modifier = Modifier.clickable { onNavigateToReports() }
            )

            HorizontalDivider()
            
            Text("Set the amount of signatures needed", style = MaterialTheme.typography.titleMedium)
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Signatures: ${petition.targetSignatures}")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { petition = petition.copy(targetSignatures = (petition.targetSignatures - 1).coerceAtLeast(petition.category.minSignatures)) }
                    ) {
                        Text("-", style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(
                        onClick = { petition = petition.copy(targetSignatures = (petition.targetSignatures + 1).coerceAtMost(1000)) }
                    ) {
                        Text("+", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            Text(
                "The minimum amount of signatures depends of the category. For the chosen category, the minimum amount of signatures is ${petition.category.minSignatures}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
