package app.reportamelo.communityIssues.ui.components.wizard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.reportamelo.communityIssues.R

@Composable
fun TipsDemo() {
    var showTip by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        showTip = true
    }

    Column {
        Text("Demo")
        if (showTip) {
            Card {
                Text("Attach photos: You can take up to 6 photos")
            }
        }
        Button(onClick = { showTip = false }) {
            Text(stringResource(R.string.hello))
        }
    }
}
