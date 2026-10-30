package app.reportamelo.communityIssues.ui.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Comment(val id: String, val text: String)
enum class CommentForType { REPORT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSectionView(
    commentFor: CommentForType,
    resourceId: String,
    title: String,
    subtitle: String,
    onNavigateBack: () -> Unit
) {
    var commentInput by remember { mutableStateOf("") }
    val comments = remember { listOf<Comment>() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Comments for: $title")
                        Text(subtitle, style = MaterialTheme.typography.bodySmall)
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp).navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentInput,
                        onValueChange = { commentInput = it },
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        placeholder = { Text("Add a comment") },
                        maxLines = 4
                    )
                    IconButton(
                        onClick = { /* submit comment */ commentInput = "" },
                        enabled = commentInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    ) { paddingValues ->
        if (comments.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No comments yet.", style = MaterialTheme.typography.titleMedium)
                Text("Please tell us how that problem affects you.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(comments) { comment ->
                    Text(comment.text, modifier = Modifier.padding(vertical = 8.dp))
                    Divider()
                }
            }
        }
    }
}
