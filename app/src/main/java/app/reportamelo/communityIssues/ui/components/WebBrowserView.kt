package app.reportamelo.communityIssues.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*


import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebBrowserView(
    modifier: Modifier = Modifier,
    url: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    val initialUrl = url ?: "https://reportamelo.app/"

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(Uri.parse(initialUrl).host ?: "Website") },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (webView?.canGoBack() == true) {
                            webView?.goBack()
                        } else {
                            onDismiss()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, webView?.url ?: initialUrl)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { webView?.reload() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reload")
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webView?.url ?: initialUrl))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = "Open in Browser")
                    }
                }
            )
        }
    ) { paddingValues ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val requestUrl = request?.url
                            if (requestUrl?.host?.contains("reportamelo.app") == true) {
                                return false
                            } else if (requestUrl?.scheme == "http" || requestUrl?.scheme == "https") {
                                val intent = Intent(Intent.ACTION_VIEW, requestUrl)
                                ctx.startActivity(intent)
                                return true
                            }
                            return true
                        }
                    }
                    loadUrl(initialUrl)
                    webView = this
                }
            }
        )
    }
}
