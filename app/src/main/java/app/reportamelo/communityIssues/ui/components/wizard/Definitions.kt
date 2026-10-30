package app.reportamelo.communityIssues.ui.components.wizard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.reportamelo.communityIssues.ui.theme.LightPrimary

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
    val title: String,
    val description: String,
    val icon: ImageVector
)

val stepsMetadata = mapOf(
    "Location" to StepsMetadata(
        title = "Location",
        description = "Where did it happen?",
        icon = Icons.Filled.LocationOn
    ),
    "Attachments" to StepsMetadata(
        title = "Attachments",
        description = "Add photos or videos",
        icon = Icons.Outlined.AddPhotoAlternate
    ),
    "Details" to StepsMetadata(
        title = "Details",
        description = "What happened?",
        icon = Icons.Outlined.EditNote
    ),
    "Confirmation" to StepsMetadata(
        title = "Confirmation",
        description = "We received your report. Thanks!",
        icon = Icons.Filled.CheckCircle
    )
)
