package app.reportamelo.communityIssues.ui.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetitionsTabView() {
    Row(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.weight(1f),
            topBar = {
                TopAppBar(
                    title = { Text("Menu") },
                    actions = {
                        IconButton(onClick = { /* Settings tapped */ }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                    title = { Text("Content") },
                    actions = {
                        IconButton(onClick = { /* Settings tapped */ }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                Text("Detail View", modifier = Modifier.padding(16.dp))
            }
        }
    }
}
