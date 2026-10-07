package app.reportamelo.models

import app.reportamelo.commons.CommentForType
import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: String? = null,
    val name: String,
    val userName: String,
    val profilePicture: String,
    val profileId: String,
    val commentFor: CommentForType,
    val resourceId: String,
    val message: String,
    val createdAt: String,
    val updatedAt: String? = null,
    val observation: String? = null,
    val action: String? = null
)
