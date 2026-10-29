package app.reportamelo.models

import kotlinx.serialization.Serializable


@Serializable
data class ReportToShare(
    val reportId: String,
    val title: String,
    val city: String,
    val country: String,
    val description: String,
    val severity: String,
    val issueType: String,
    val status: String,
    val coordinate: Coordinate,
    val cellIndex: String,
    val openCodeLocation: String,
    val attachments: List<PreviewAttachmentRequest>
)

@Serializable
enum class ShareType {
    report,
    petition
}

@Serializable
data class Share<T>(
    val payload: T,
    val shareType: ShareType,
    val lang: String
)

@Serializable
data class ShareUrlResponse(
    val shareUrl: String
)
