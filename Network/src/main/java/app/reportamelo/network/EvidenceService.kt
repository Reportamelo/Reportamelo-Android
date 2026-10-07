package app.reportamelo.network

import app.reportamelo.models.*

class EvidenceService(private val client: ServiceClient = ServiceClient(baseURL = app.reportamelo.commons.Apis.apiV1)) {
    
    suspend fun publishExternalContributions(
        attachments: List<GroupedAttachmentPayload>,
        headers: Map<String, String>
    ): CustomizedResponse<List<ReportAttachmentGrouping>> {
        return client.post(
            path = "external-contributions/group/by/container",
            body = attachments,
            headers = headers,
            withOAuth = true
        )
    }
}
