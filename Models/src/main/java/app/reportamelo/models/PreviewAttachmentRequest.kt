package app.reportamelo.models

import app.reportamelo.commons.AttachmentType
import kotlinx.serialization.Serializable

@Serializable
data class PreviewAttachmentRequest(
    var attachmentId: String = "",
    var fileName: String = "",
    var type: AttachmentType = AttachmentType.IMAGE,
    var key: String = "",
    var notes: String? = null,
    var reportContainer: String = ""
)
