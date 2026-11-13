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
import androidx.compose.ui.res.stringResource
import app.reportamelo.communityIssues.R
import androidx.annotation.StringRes

// Mocks
enum class PetitionCategory(val title: String, @StringRes val titleRes: Int, val minSignatures: Int) {
    ALL("Select a category", R.string.select_a_category, 0),
    ENVIRONMENT("Environment", R.string.environment, 50),
    INFRASTRUCTURE("Infrastructure", R.string.infrastructure, 100)
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
                title = { Text(stringResource(R.string.create_petition)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
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
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.submit))
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
            Text(stringResource(R.string.details_of_the_request), style = MaterialTheme.typography.titleMedium)
            
            OutlinedTextField(
                value = petition.title,
                onValueChange = { petition = petition.copy(title = it) },
                label = { Text(stringResource(R.string.title)) },
                placeholder = { Text(stringResource(R.string.please_enter_a_title_text)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = petition.description,
                onValueChange = { petition = petition.copy(description = it) },
                label = { Text(stringResource(R.string.description)) },
                placeholder = { Text(stringResource(R.string.please_enter_a_description_text)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = stringResource(petition.category.titleRes),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.category)) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    PetitionCategory.values().forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(stringResource(selectionOption.titleRes)) },
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
            
            Text(stringResource(R.string.reports), style = MaterialTheme.typography.titleMedium)
            
            ListItem(
                headlineContent = { Text(stringResource(R.string.choose_report_s)) },
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
            
            Text(stringResource(R.string.set_the_amount_of_signatures_needed), style = MaterialTheme.typography.titleMedium)
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.signatures_d, petition.targetSignatures))
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
                stringResource(R.string.minimum_signatures_for_category_d, petition.category.minSignatures),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
