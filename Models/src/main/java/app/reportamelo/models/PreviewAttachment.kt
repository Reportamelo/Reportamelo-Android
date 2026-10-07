package app.reportamelo.models

import kotlinx.serialization.Serializable
import app.reportamelo.commons.*

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
    val reportContainer: String
)
