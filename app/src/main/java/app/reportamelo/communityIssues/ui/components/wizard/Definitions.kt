package app.reportamelo.communityIssues.ui.components.wizard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.reportamelo.communityIssues.ui.theme.LightPrimary
import androidx.annotation.StringRes
import app.reportamelo.communityIssues.R

enum class ReportStep(val rawValue: Int, val metadataKey: String) {
    LOCATION(1, "Location"),
    MEDIA(2, "Attachments"),
    DETAILS(3, "Details"),
    CONFIRMATION(4, "Confirmation");

    val color: Color
        get() = when (this) {
            LOCATION, MEDIA, DETAILS -> LightPrimary
            CONFIRMATION -> Color(0xFF34C759)
        }

    companion object {
        fun fromRawValue(rawValue: Int): ReportStep? {
            return entries.find { it.rawValue == rawValue }
        }
    }
}

data class StepsMetadata(
    val id: String = java.util.UUID.randomUUID().toString(),
    @StringRes val title: Int,
    @StringRes val description: Int,
    val icon: ImageVector
)

val stepsMetadata = mapOf(
    "Location" to StepsMetadata(
        title = R.string.location,
        description = R.string.where_did_it_happen,
        icon = Icons.Filled.LocationOn
    ),
    "Attachments" to StepsMetadata(
        title = R.string.attachments,
        description = R.string.add_photos_or_videos,
        icon = Icons.Outlined.AddPhotoAlternate
    ),
    "Details" to StepsMetadata(
        title = R.string.details,
        description = R.string.what_happened,
        icon = Icons.Outlined.EditNote
    ),
    "Confirmation" to StepsMetadata(
        title = R.string.confirmation,
        description = R.string.we_received_your_report_thanks,
        icon = Icons.Filled.CheckCircle
    )
)
