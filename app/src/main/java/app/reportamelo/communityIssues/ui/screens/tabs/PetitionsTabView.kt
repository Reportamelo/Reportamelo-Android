package app.reportamelo.communityIssues.ui.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import app.reportamelo.communityIssues.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetitionsTabView() {
    Row(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.weight(1f),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.menu)) },
                    actions = {
                        IconButton(onClick = { /* Settings tapped */ }) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                Text("Sidebar Item", modifier = Modifier.padding(16.dp))
            }
        }

        Divider(modifier = Modifier.width(1.dp).fillMaxHeight())

        Scaffold(
            modifier = Modifier.weight(2f),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.content)) },
                    actions = {
                        IconButton(onClick = { /* Settings tapped */ }) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                Text(stringResource(R.string.detail_view), modifier = Modifier.padding(16.dp))
            }
        }
    }
}
