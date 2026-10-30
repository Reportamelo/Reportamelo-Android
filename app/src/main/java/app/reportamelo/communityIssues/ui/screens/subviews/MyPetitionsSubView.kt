package app.reportamelo.communityIssues.ui.screens.subviews

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

import app.reportamelo.models.PetitionPost
import app.reportamelo.commons.ViewOptions
import app.reportamelo.communityIssues.ui.components.LoadingView

@Composable
fun PetitionCellView(petition: PetitionPost) {
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Text(
            text = petition.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = petition.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPetitionsSubView(subViewName: String, mode: ViewOptions = ViewOptions.LIST) {
    var isLoading by remember { mutableStateOf(false) }
    var petitions by remember { mutableStateOf<List<PetitionPost>>(emptyList()) }

    LaunchedEffect(Unit) {
        isLoading = true
        delay(500) // Simulate network call
        // Mock data empty for compilation
        petitions = emptyList()
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(subViewName) })
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingView()
                }
            } else if (petitions.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.PersonOutline, contentDescription = null, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No petitions yet.", style = MaterialTheme.typography.titleLarge)
                    Text("Please create petitions to accelerate the process...")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    items(petitions) { petition ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            PetitionCellView(petition = petition)
                        }
                    }
                }
            }
        }
    }
}
