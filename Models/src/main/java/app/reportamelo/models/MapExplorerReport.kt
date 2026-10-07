package app.reportamelo.models

import kotlinx.serialization.Serializable

@Serializable
data class MapExplorerReport(
    val id: String,
    val lat: Double,
    val lng: Double,
    val address: String,
    val title: String,
    val description: String,
    val severityId: Int,
    val statusId: Int,
    val issueTypeId: Int,
    val matterToSolveId: Int,
    val reportedAtRaw: Long? = null,
    val cellIndex: String,
    val createdAtRaw: Long,
    val updatedAtRaw: Long,
    val reportedBy: String? = null,
    val userName: String,
    val cityId: String,
    val petitionId: String? = null,
    val shareUrl: String,
    val attachments: List<PreviewAttachment>,
    val assignedTo: String? = null,
    val institutionId: String? = null,
    val reportContainer: String,
    val profileId: String
)
