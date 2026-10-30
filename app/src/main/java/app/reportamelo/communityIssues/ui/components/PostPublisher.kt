package app.reportamelo.communityIssues.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*


import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import app.reportamelo.models.PetitionPost
import app.reportamelo.models.User

@Composable
fun AvatarView(user: User, modifier: Modifier = Modifier) {
    AsyncImage(
        model = user.profilePicture,
        contentDescription = "Avatar",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
    )
}

@Composable
fun Signatories(users: List<User>) {
    Row(horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
        users.forEach { user ->
            AvatarView(
                user = user,
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
        }
    }
}

@Composable
fun PostPublisher(petition: PetitionPost) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        AvatarView(user = petition.postPublisher)
        
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = petition.postPublisher.names,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@${petition.postPublisher.userName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            val publishedInfo = "${"Just now"} - ${petition.postMetadata.city}, ${petition.postMetadata.countryCode}"
            Text(
                text = publishedInfo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
        }
    }
}
