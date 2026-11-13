package app.reportamelo.communityIssues.ui.screens.subviews

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.UUID
import androidx.compose.ui.res.stringResource
import app.reportamelo.communityIssues.R
import app.reportamelo.communityIssues.helpers.localized

data class License(val id: String = UUID.randomUUID().toString(), val library: String, val text: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesSubView(
    modifier: Modifier = Modifier,
    subViewName: String = stringResource(R.string.licenses),
    onBack: (() -> Unit)? = null
) {
    var expandedLicenseIDs by remember { mutableStateOf(setOf<String>()) }
    var expandAll by remember { mutableStateOf(true) }
    
    val licenses = remember {
        listOf(
            License(library = "Google SignIn", text = localized(R.string.license_text_not_available, "License text not available.")),
            License(library = "Libwebp", text = localized(R.string.license_text_not_available, "License text not available.")),
            License(library = "Promises", text = localized(R.string.license_text_not_available, "License text not available.")),
            License(library = "WebP", text = localized(R.string.license_text_not_available, "License text not available.")),
            License(library = "AppAuth", text = localized(R.string.license_text_not_available, "License text not available.")),
            License(library = "Photos from Unsplash", text = localized(R.string.license_text_not_available, "License text not available."))
        )
    }

    LaunchedEffect(Unit) {
        expandedLicenseIDs = licenses.map { it.id }.toSet()
    }

    LaunchedEffect(expandAll) {
        if (expandAll) {
            expandedLicenseIDs = licenses.map { it.id }.toSet()
        } else {
            expandedLicenseIDs = emptySet()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(subViewName) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { expandAll = !expandAll }) {
                        Icon(
                            imageVector = if (expandAll) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                            contentDescription = stringResource(R.string.expand_collapse_all)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            items(licenses, key = { it.id }) { license ->
                val isExpanded = expandedLicenseIDs.contains(license.id)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedLicenseIDs = if (isExpanded) {
                                    expandedLicenseIDs - license.id
                                } else {
                                    expandedLicenseIDs + license.id
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(license.library, modifier = Modifier.weight(1f))
                        Text(
                            text = if (isExpanded) stringResource(R.string.hide) else stringResource(R.string.show),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (isExpanded) {
                        Text(license.text, style = MaterialTheme.typography.bodySmall)
                        Divider(modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
            }
        }
    }
}
