package app.reportamelo.communityIssues.ui.screens.subviews

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignedPetitionsSubView(subViewName: String) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(subViewName) })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Empty list as per original swift file
            }
            Text("Signed Petitions List (Empty)")
        }
    }
}
