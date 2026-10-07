package app.reportamelo.models

import app.reportamelo.commons.CommentForType
import kotlinx.serialization.Serializable

@Serializable
data class CommentToBlock(
    val id: String,
    val message: String,
    val profileId: String,
    val commentFor: CommentForType,
    val resourceId: String
)
