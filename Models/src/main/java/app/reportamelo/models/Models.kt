package app.reportamelo.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import app.reportamelo.commons.*

@Serializable
data class User(
    val names: String,
    val userName: String,
    val profilePicture: String,
    val profileId: String,
    val hideProfile: Boolean
)

@Serializable
data class GroupedAttachmentPayload(
    val attachmentContainer: String,
    val key: String,
    val previewFileName: String,
    val fileName: String,
    val reportId: String,
    val notes: String
)

@Serializable
data class CustomizedResponse<T>(
    val message: String,
    val code: String,
    val data: T
)

@Serializable
data class ReportAttachmentGrouping(
    val attachmentId: String,
    val attachmentContainer: String,
    val key: String
)

@Serializable
data class InsightsFilter(
    val year: String,
    val month: String
)

@Serializable
data class BasicInfo(
    val id: String,
    val title: String,
    val status: String
)

@Serializable
data class DaySummary(
    val interactions: Int,
    val reports: List<BasicInfo>,
    val signatures: List<BasicInfo>,
    val comments: List<BasicInfo>,
    val petitions: List<BasicInfo>
)

@Serializable
data class MonthlyInsightsResponse(
    val totalReports: Int,
    val totalSignatures: Int,
    val totalComments: Int,
    val totalPetitions: Int,
    val recentActivity: Map<String, DaySummary>
)

@Serializable
data class PostMetadata(
    val audience: String,
    val visibility: PostVisibility,
    val countryCode: CountryCode,
    val city: String,
    val cityId: String,
    val language: String,
    val shareLink: String
)

@Serializable
data class ReportMetadata(
    val id: String,
    val lat: Double,
    val lng: Double,
    val severityId: Int,
    val statusId: Int,
    val issueTypeId: Int,
    val matterToSolveId: Int
)

@Serializable
data class PostSigners(
    val hasCurrentUserSigned: Boolean = false,
    val latestsSigners: List<User> = emptyList()
)

@Serializable
data class Petition(
    val id: String? = null,
    val title: String,
    val description: String,
    val targetSignatures: Int,
    val currentSignatures: Int? = null,
    val categoryId: Int,
    val statusId: Int? = null,
    val reportedBy: String? = null,
    val disabled: Boolean? = null,
    val reportsIds: List<String> = emptyList()
)

@Serializable
data class PetitionDTO(
    val id: String? = null,
    val title: String,
    val description: String,
    val targetSignatures: Int,
    val currentSignatures: Int? = null,
    val categoryId: Int,
    val statusId: Int? = null,
    val reportedBy: String? = null,
    val disabled: Boolean? = null,
    val reportsIds: List<String> = emptyList()
)

@Serializable
data class PetitionPost(
    val id: String,
    val title: String,
    val description: String,
    val targetSignatures: Int,
    val currentSignatures: Int,
    val categoryId: Int,
    val statusId: Int,
    val reportedBy: String? = null,
    val disabled: Boolean,
    val reportsIds: List<String> = emptyList(),
    val attachments: List<PreviewAttachment> = emptyList(),
    val postMetadata: PostMetadata,
    val postPublisher: User,
    val reportsMetadata: List<ReportMetadata> = emptyList(),
    val postSigners: PostSigners,
    val progress: Double
)

@Serializable
data class ReportSessionResponse(
    val reportContainer: String,
    val shareIndexHash: String,
    val reportCreationOn: String
)

typealias StartReportResponse = CustomizedResponse<ReportSessionResponse>

@Serializable
data class Report(
    val id: String? = null,
    val lat: Double,
    val lng: Double,
    val issueTypeId: Int,
    val severityId: Int,
    val statusId: Int,
    val zipCode: String,
    val groupingId: String? = null,
    val reportedBy: String? = null,
    val suggestedTitle: String,
    val suggestedDescription: String,
    val attachments: List<String> = emptyList(),
    val cityId: String,
    val reportContainer: String? = null,
    val shareUrl: String? = null,
    val observations: String? = null
)

@Serializable
data class ReportDAO(
    val id: String? = null,
    val lat: Double,
    val lng: Double,
    val issueTypeId: Int,
    val severityId: Int,
    val statusId: Int,
    val zipCode: String,
    val groupingId: String? = null,
    val reportedBy: String? = null,
    val suggestedTitle: String,
    val suggestedDescription: String,
    val attachments: List<String> = emptyList(),
    val cityId: String,
    val reportContainer: String? = null,
    val shareUrl: String? = null,
    val observations: String? = null
)

@Serializable
data class AttachMediaResponse(
    val name: String,
    val status: String,
    val key: String
)

@Serializable
data class AssignedInstitution(
    val institutionName: String,
    val institutionCode: String,
    val institutionId: String
)

@Serializable
data class ResolutionMetadata(
    val cityId: String,
    val groupingId: String,
    val assigned: AssignedInstitution,
    val resourceType: String,
    val resourceId: String
)

@Serializable
data class Attachment(
    @SerialName("attachmentId") val id: String? = null,
    val type: AttachmentType,
    @SerialName("createdAt") val createdAtRaw: Long,
    @SerialName("updatedAt") val updatedAtRaw: Long? = null,
    val uploaderUserName: String,
    @SerialName("validatedAt") val validatedAtRaw: Long? = null,
    val validatedBy: AttachmentValidatedBy? = null,
    val state: ReportAttachmentState,
    val notes: String,
    val key: String? = null,
    val fileName: String? = null,
    val reportContainer: String? = null
)

@Serializable
data class IssueUpdate(
    val id: String? = null,
    val date: String,
    val by: String,
    val comments: String,
    val status: String,
    val attachments: List<Attachment>
)

@Serializable
data class Milestone(
    val date: String? = null,
    val by: String? = null,
    val comments: String? = null,
    val attachments: List<Attachment>? = null
)

@Serializable
data class InProgressMilestone(
    val assignedInstitution: String,
    val updates: List<IssueUpdate>
)

@Serializable
data class IssueHistory(
    val reported: Milestone? = null,
    val confirmed: Milestone? = null,
    val inProgress: InProgressMilestone? = null,
    val fixed: Milestone? = null
)

@Serializable
data class Resolution(
    val status: String,
    val id: String,
    val history: IssueHistory,
    val metadata: ResolutionMetadata
)


@Serializable
data class UserProfile(
    val username: String? = null,
    val avatar: String? = null,
    val email: String? = null,
    val profileId: String = ""
)
