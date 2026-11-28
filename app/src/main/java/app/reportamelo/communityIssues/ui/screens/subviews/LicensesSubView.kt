package app.reportamelo.communityIssues.ui.screens.subviews

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.reportamelo.communityIssues.R
import app.reportamelo.communityIssues.controllers.LicensesViewModel
import app.reportamelo.communityIssues.datamodel.AssetLicenseSource
import app.reportamelo.communityIssues.datamodel.License

/** Room under the list for the floating tab bar this screen is shown under (same as CreateReportView). */
private val FloatingTabBarClearance = 108.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesSubView(
    modifier: Modifier = Modifier,
    subViewName: String = stringResource(R.string.licenses),
    onBack: (() -> Unit)? = null,
    viewModel: LicensesViewModel = run {
        val assets = LocalContext.current.assets
        viewModel { LicensesViewModel(AssetLicenseSource(assets)) }
    }
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(subViewName) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::toggleAll,
                        enabled = state.licenses.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = if (state.allExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                            contentDescription = stringResource(R.string.expand_collapse_all)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding())

        when {
            state.isLoading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.licenses.isEmpty() -> Box(
                contentModifier.padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.license_text_not_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Every paragraph is its own lazy item: the full set is ~260KB of text, and a single
            // Text per license would lay out thousands of lines at once.
            else -> LazyColumn(
                modifier = contentModifier,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = FloatingTabBarClearance
                )
            ) {
                state.licenses.forEach { license ->
                    val isExpanded = license.id in state.expandedIds

                    item(key = license.id, contentType = "header") {
                        LicenseHeader(
                            library = license.library,
                            isExpanded = isExpanded,
                            onToggle = { viewModel.toggle(license.id) }
                        )
                    }

                    if (isExpanded) {
                        items(
                            count = license.paragraphs.size,
                            key = { index -> "${license.id}#$index" },
                            contentType = { "paragraph" }
                        ) { index ->
                            LicenseParagraph(license = license, index = index)
                        }
                        item(key = "${license.id}#divider", contentType = "divider") {
                            HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LicenseHeader(
    modifier: Modifier = Modifier,
    library: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(library, modifier = Modifier.weight(1f))
        Text(
            text = if (isExpanded) stringResource(R.string.hide) else stringResource(R.string.show),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun LicenseParagraph(
    modifier: Modifier = Modifier,
    license: License,
    index: Int
) {
    val paragraph = license.paragraphs[index]
    val linkColor = MaterialTheme.colorScheme.primary
    // Credits files carry <a href> links: render them as tappable links, not raw markup
    val text = remember(paragraph, license.isHtml, linkColor) {
        if (license.isHtml) {
            AnnotatedString.fromHtml(
                htmlString = paragraph,
                linkStyles = TextLinkStyles(
                    style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
                )
            )
        } else {
            AnnotatedString(paragraph)
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier.padding(bottom = 8.dp)
    )
}
