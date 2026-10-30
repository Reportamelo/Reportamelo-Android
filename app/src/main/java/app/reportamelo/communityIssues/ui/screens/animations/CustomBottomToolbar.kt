package app.reportamelo.communityIssues.ui.screens.animations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import app.reportamelo.repository.UserRepository

@Composable
fun CustomBottomToolbar(
    commentAction: () -> Unit,
    addPhotoAction: () -> Unit,
    affectedAction: (Boolean) -> Unit,
    boostReportValidationAction: (Boolean) -> Unit,
    affectedState: Boolean,
    onAffectedStateChange: (Boolean) -> Unit,
    notificationState: Boolean,
    onNotificationStateChange: (Boolean) -> Unit,
    disableBoostButton: Boolean
) {
    var isGuest by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isGuest = UserRepository.shared.isGuestUser()
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Comment Button
        Button(
            onClick = commentAction,
            enabled = !isGuest,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(64.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "CommentReportButton",
                    tint = if (isGuest) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Comment",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isGuest) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Evidences Button
        Button(
            onClick = addPhotoAction,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(72.dp, 64.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.AddAPhoto,
                    contentDescription = "AddAttachmentsButton",
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Evidences",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Boost Button
        Button(
            onClick = {
                val newState = !notificationState
                onNotificationStateChange(newState)
                boostReportValidationAction(newState)
            },
            enabled = !isGuest && !disableBoostButton,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(68.dp, 64.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "BoostButton",
                    tint = if (disableBoostButton) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (disableBoostButton) "Boosted" else "Boost",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isGuest) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
