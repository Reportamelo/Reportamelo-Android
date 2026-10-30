package app.reportamelo.communityIssues.ui.screens.tabs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Mock Data
data class Citizen(val names: String, val userName: String, val profilePictureURL: String?)
data class Report(val id: String, val title: String)
data class Petition(val id: String, val title: String)

enum class AppTab(val title: String) {
    REPORTS("Reports"),
    PETITIONS("Petitions")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenProfile(profileId: String, onNavigateBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(AppTab.REPORTS) }
    val citizen = remember { Citizen("Francisco Hernandez", "fran", null) }
    val reports = remember { listOf<Report>() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* More actions */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp))
            }
            
            Text(
                text = citizen.names,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Text(
                text = "@${citizen.userName}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text("0 Reports submitted", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("0 Petitions published", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                AppTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.title) }
                    )
                }
            }
            
            Box(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                when (selectedTab) {
                    AppTab.REPORTS -> {
                        if (reports.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No reports yet.", style = MaterialTheme.typography.titleMedium)
                                Text("Please sent us reports.", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            // List reports
                        }
                    }
                    AppTab.PETITIONS -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No petitions yet.", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
