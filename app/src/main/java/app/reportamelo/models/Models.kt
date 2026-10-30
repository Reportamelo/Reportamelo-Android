package app.reportamelo.models

enum class AttachmentState {
    PENDING, CONFIRMED, INAPPROPRIATE, DELETED, MANUAL_REVISION
}

data class Milestone(
    val title: String,
    val date: String,
    val description: String
)
