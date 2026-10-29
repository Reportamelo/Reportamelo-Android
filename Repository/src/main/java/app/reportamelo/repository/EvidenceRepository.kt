package app.reportamelo.repository

import app.reportamelo.models.*
import app.reportamelo.commons.*
import app.reportamelo.network.*

object EvidenceRepository {

    private val service = app.reportamelo.network.EvidenceService(app.reportamelo.network.ServiceClient(Apis.apiV1))
    var headers = mapOf(Apis.CLIENT_TYPE_HEADER to Apis.CLIENT_TYPE_ANDROID, "CountryCode" to "SV")

    suspend fun publishExternalContributions(attachments: List<GroupedAttachmentPayload>, onComplete: (Result<CustomizedResponse<List<ReportAttachmentGrouping>>>) -> Unit) {
        try {
            val res = service.publishExternalContributions(attachments, headers)
            onComplete(Result.success(res))
        } catch (e: Exception) {
            onComplete(Result.failure(e))
        }
    }
}
