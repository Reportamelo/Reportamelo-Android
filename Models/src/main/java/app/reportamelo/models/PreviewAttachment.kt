package app.reportamelo.models

import android.net.Uri
import kotlinx.serialization.Serializable
import app.reportamelo.commons.*
import app.reportamelo.commons.utils.buildPreviewAttachmentURL

@Serializable
data class PreviewAttachment(
    val id: String,
    val type: AttachmentType,
    val createdAtRaw: Long,
    val updatedAtRaw: Long? = null,
    val uploaderUserName: String,
    val validatedBy: AttachmentValidatedBy? = null,
    val state: ReportAttachmentState,
    val fileName: String,
    val reportContainer: String,
    val validatedAtRaw: Long? = null,
    val notes: String? = null
) {
    val url: Uri?
        get() = if (reportContainer.isNotEmpty() && fileName.isNotEmpty()) {
            buildPreviewAttachmentURL(reportContainer, fileName, state, updatedAtRaw)
        } else {
            null
        }
}
