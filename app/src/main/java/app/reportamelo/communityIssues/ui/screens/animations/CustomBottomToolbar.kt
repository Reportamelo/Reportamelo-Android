package app.reportamelo.communityIssues.ui.screens.animations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.reportamelo.communityIssues.R
import app.reportamelo.repository.UserRepository

private val BoostFlameOrange = Color(0xFFFF6D00)

@Composable
fun CustomBottomToolbar(
    modifier: Modifier = Modifier,
    commentAction: () -> Unit,
    addPhotoAction: () -> Unit,
    affectedAction: (Boolean) -> Unit = {},
    boostReportValidationAction: (Boolean) -> Unit = {},
    affectedState: Boolean = false,
    onAffectedStateChange: (Boolean) -> Unit = {},
    notificationState: Boolean = false,
    onNotificationStateChange: (Boolean) -> Unit = {},
    disableBoostButton: Boolean = false,
    isBoostLoading: Boolean = false,
    isGuestOverride: Boolean? = null
) {
    var isGuest by remember { mutableStateOf(isGuestOverride ?: false) }

    LaunchedEffect(isGuestOverride) {
        isGuest = isGuestOverride ?: UserRepository.shared.isGuestUser()
    }

    val capsuleShape = RoundedCornerShape(50)

    Row(
        modifier = modifier
            .padding(horizontal = 24.dp)
            .shadow(elevation = 12.dp, shape = capsuleShape)
            .clip(capsuleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp, horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Comment Button
        Button(
            onClick = commentAction,
            enabled = !isGuest,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "CommentReportButton",
                    tint = if (isGuest) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.comment),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isGuest) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }

        // Evidences Button
        Button(
            onClick = addPhotoAction,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "AddAttachmentsButton",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.evidences),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Boost Button
        val boostDisabled = isGuest || disableBoostButton || isBoostLoading
        Button(
            onClick = {
                val newState = !notificationState
                onNotificationStateChange(newState)
                boostReportValidationAction(newState)
            },
            enabled = !boostDisabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isBoostLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = BoostFlameOrange
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "BoostButton",
                        tint = if (isGuest) {
                            BoostFlameOrange.copy(alpha = 0.4f)
                        } else {
                            BoostFlameOrange
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (disableBoostButton) {
                        stringResource(R.string.boosted)
                    } else {
                        stringResource(R.string.boost)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (boostDisabled) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}
