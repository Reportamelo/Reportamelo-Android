package app.reportamelo.communityIssues.ui.screens.attachments

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rizzi.bouquet.HorizontalPDFReader
import com.rizzi.bouquet.ResourceType
import com.rizzi.bouquet.rememberHorizontalPdfReaderState

@Composable
fun PDFViewer(url: String, modifier: Modifier = Modifier) {
    val pdfState = rememberHorizontalPdfReaderState(
        resource = ResourceType.Remote(url),
        isZoomEnable = true
    )
    HorizontalPDFReader(
        state = pdfState,
        modifier = modifier.fillMaxSize()
    )
}
