package app.reportamelo.repository

import app.reportamelo.models.*
import app.reportamelo.commons.*
import app.reportamelo.network.*

object EvidenceRepository {

    private val service = app.reportamelo.network.EvidenceService(app.reportamelo.network.ServiceClient("https://api.reportamelo.app/v1/"))
    var headers = mapOf("Client-Type" to "Mobile-App", "CountryCode" to "SV")

    suspend fun publishExternalContributions(attachments: List<GroupedAttachmentPayload>, onComplete: (Result<CustomizedResponse<List<ReportAttachmentGrouping>>>) -> Unit) {
        try {
            val res = service.publishExternalContributions(attachments, headers)
            onComplete(Result.success(res))
        } catch (e: Exception) {
            onComplete(Result.failure(e))
        }
    }
}
