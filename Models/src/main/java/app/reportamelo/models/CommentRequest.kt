package app.reportamelo.models

import kotlinx.serialization.Serializable

@Serializable
data class CommentRequest(
    val reportId: String,
    val message: String
)
