package app.reportamelo.communityIssues.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.reportamelo.commons.utils.formatRelativeEpochMillis
import app.reportamelo.communityIssues.R
import app.reportamelo.models.MapExplorerReport
import java.util.Locale
import kotlin.math.abs

@Composable
fun MoreInformationView(
    modifier: Modifier = Modifier,
    report: MapExplorerReport,
    onShowProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val opacity = 0.85f

    val createdOn = report.createdAtRaw.takeIf { it > 0L }?.let { raw ->
        val epochMillis = if (raw < 100_000_000_000L) raw * 1000L else raw
        formatRelativeEpochMillis(epochMillis)
    } ?: ""

    val lastUpdate = report.updatedAtRaw.takeIf { it > 0L }?.let { raw ->
        val epochMillis = if (raw < 100_000_000_000L) raw * 1000L else raw
        formatRelativeEpochMillis(epochMillis)
    } ?: ""

    val displayUserName = report.userName.ifEmpty { "guest" }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.more_information),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Report Id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Report ID", report.id))
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.report_id),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = opacity)
                )
                Text(
                    text = report.id,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Reported by
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShowProfile(report.profileId) }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.reported_by),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = opacity)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "@$displayUserName",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.reported_by),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Created on
            InfoRow(
                label = stringResource(R.string.created_on),
                value = createdOn
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Last update
            InfoRow(
                label = stringResource(R.string.last_update),
                value = lastUpdate
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Address
            InfoRow(
                label = stringResource(R.string.address_label),
                value = report.address,
                maxLines = 2
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Coordinates
            val latFormatted = String.format(Locale.US, "%.6f", abs(report.lat))
            val lngFormatted = String.format(Locale.US, "%.6f", abs(report.lng))
            val latDir = if (report.lat >= 0) "N" else "S"
            val lngDir = if (report.lng >= 0) "E" else "W"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openCoordinatesInGoogleMaps(context, report.lat, report.lng) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.coordinates),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = opacity)
                )
                Text(
                    text = buildAnnotatedString {
                        append("$latFormatted° ")
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(latDir)
                        }
                        append(", $lngFormatted° ")
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(lngDir)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

fun openCoordinatesInGoogleMaps(context: Context, lat: Double, lng: Double) {
    val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    if (mapIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(mapIntent)
    } else {
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}

@Composable
private fun InfoRow(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    maxLines: Int = 1
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = maxLines,
            textAlign = TextAlign.End
        )
    }
}
