package app.reportamelo.communityIssues.ui.screens.attachments

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil.compose.AsyncImage

@Composable
fun AttachmentDetailView(url: String, isPdf: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        if (isPdf) {
            PDFViewer(url = url, modifier = Modifier.fillMaxSize())
        } else {
            AsyncImage(
                model = url,
                contentDescription = "Attachment Image",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
