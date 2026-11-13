package app.reportamelo.communityIssues.ui.components.wizard

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.reportamelo.communityIssues.ui.components.ThemedButton
import app.reportamelo.communityIssues.ui.components.ThemedButtonType
import app.reportamelo.communityIssues.ui.theme.Dimensions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import app.reportamelo.communityIssues.R
import app.reportamelo.communityIssues.helpers.localized

@Composable
fun ConfirmationView(
    modifier: Modifier = Modifier,
    id: String,
    url: String
) {
    var showCopiedCodeMicroInteraction by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val displayId = id.ifEmpty { "SV-SSC-261027-aXWsaxls" }
    val resolvedShareUrl = url.ifEmpty { "https://reportamelo.app/s/$displayId" }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimensions.ThemeSpacing * 5)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimensions.ThemeSpacing * 3)
        ) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = stringResource(R.string.submitted),
                tint = Color(0xFF34C759),
                modifier = Modifier.size(64.dp)
            )

            Text(
                text = stringResource(R.string.your_report_has_been_submitted),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.please_wait_for_the_report_to_be_reviewed_we_will_notify_you_in_each_s),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.response_time_can_vary_depending_on_the_institution_severity_and_type_of_the_report),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.meanwhile_you_can_copy_report_code_or_share_it_with_others),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimensions.ThemeSpacing * 3)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Text(
                    text = displayId,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ThemedButton(
                        message = if (showCopiedCodeMicroInteraction) stringResource(R.string.copied) else stringResource(R.string.copy_report_code),
                        icon = if (showCopiedCodeMicroInteraction) Icons.Filled.CheckCircle else Icons.Filled.ContentCopy,
                        type = ThemedButtonType.OUTLINE,
                        onClick = {
                            clipboardManager.setText(AnnotatedString(displayId))
                            coroutineScope.launch {
                                showCopiedCodeMicroInteraction = true
                                delay(2000)
                                showCopiedCodeMicroInteraction = false
                            }
                        }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    ThemedButton(
                        message = stringResource(R.string.share_report),
                        icon = Icons.Filled.Share,
                        type = ThemedButtonType.OUTLINE,
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, resolvedShareUrl)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, localized(R.string.share_report, "Share Report"))
                            context.startActivity(shareIntent)
                        }
                    )
                }
            }
        }
    }
}
