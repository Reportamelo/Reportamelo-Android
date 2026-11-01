package app.reportamelo.models

import app.reportamelo.commons.Apis
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
) {
    val url: String
        get() {
            val rawKey = key.takeIf { it.isNotBlank() } ?: return ""
            return "${Apis.baseURL.trimEnd('/')}/${rawKey.trimStart('/')}"
        }
}
