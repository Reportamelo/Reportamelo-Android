package app.reportamelo.communityIssues.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.runtime.Composable
import app.reportamelo.communityIssues.ui.theme.Dimensions

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun PostInteractions(
    hasCurrentUserSigned: Boolean = false,
    onSign: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        TextButton(
            onClick = onSign,
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(Dimensions.ThemeRadius)),
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (hasCurrentUserSigned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(imageVector = Icons.Default.Create, contentDescription = "Sign")
        }
        
        TextButton(
            onClick = onComment,
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(Dimensions.ThemeRadius)),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
        ) {
            Icon(imageVector = Icons.Outlined.ChatBubbleOutline, contentDescription = "Comment")
        }
        
        TextButton(
            onClick = onShare,
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(Dimensions.ThemeRadius)),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
        ) {
            Icon(imageVector = Icons.Filled.Share, contentDescription = "Share")
        }
    }
}
