package app.reportamelo.network.services

import app.reportamelo.commons.TypeOfContentToReport
import app.reportamelo.models.*
import app.reportamelo.network.ServiceClient

class ModerationService(private val client: ServiceClient) {

    suspend fun moderateReport(reason: ReportViolation<MapExplorerReport>, type: TypeOfContentToReport, headers: Map<String, String>): GenericResponse {
        return client.post("user-generated-content/moderation/${type.name}", reason, headers, withOAuth = true)
    }

    suspend fun moderateContent(reason: ReportViolation<PreviewAttachment>, type: TypeOfContentToReport, headers: Map<String, String>): GenericResponse {
        return client.post("user-generated-content/moderation/${type.name}", reason, headers, withOAuth = true)
    }

    suspend fun moderateComment(reason: ReportViolation<CommentToBlock>, type: TypeOfContentToReport, headers: Map<String, String>): GenericResponse {
        return client.post("user-generated-content/moderation/${type.name}", reason, headers, withOAuth = true)
    }

    suspend fun moderateAccount(reason: ReportViolation<User>, type: TypeOfContentToReport, headers: Map<String, String>): GenericResponse {
        return client.post("user-generated-content/moderation/${type.name}", reason, headers, withOAuth = true)
    }

    suspend fun myComplaints(headers: Map<String, String>): PaginatedResponse<ReportViolation<ModeratedContent>> {
        return client.get("user-generated-content/moderation/my-complaints", headers, withOAuth = true)
    }

    suspend fun indictedModeratedContent(type: TypeOfContentToReport, headers: Map<String, String>): PaginatedResponse<ReportViolation<PreviewAttachment>> {
        return client.get("user-generated-content/moderation/indicted/my-accusations/${type.name}", headers, withOAuth = true)
    }

    suspend fun indictedModeratedMessages(type: TypeOfContentToReport, headers: Map<String, String>): PaginatedResponse<ReportViolation<CommentToBlock>> {
        return client.get("user-generated-content/moderation/indicted/my-accusations/${type.name}", headers, withOAuth = true)
    }

    suspend fun indictedModeratedContentAll(headers: Map<String, String>): PaginatedResponse<ReportViolation<ModeratedContent>> {
        return client.get("user-generated-content/moderation/indicted/my-accusations/all", headers, withOAuth = true)
    }

    suspend fun appeal(id: String, type: TypeOfContentToReport, headers: Map<String, String>): GenericResponse {
        return client.put("user-generated-content/moderation/$id/appeal", emptyMap<String, String>(), headers, withOAuth = true)
    }

    suspend fun remove(id: String, type: TypeOfContentToReport, headers: Map<String, String>): GenericResponse {
        return client.delete("user-generated-content/moderation/$id", emptyMap<String, String>(), headers, withOAuth = true)
    }
}
