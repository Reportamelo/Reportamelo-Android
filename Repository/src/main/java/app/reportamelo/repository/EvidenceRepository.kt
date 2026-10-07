package app.reportamelo.repository

import app.reportamelo.commons.*
import app.reportamelo.models.*
import app.reportamelo.network.*

object EvidenceRepository {
    private val evidenceService: EvidenceService = EvidenceService()
    
    var headers: List<HTTPHeader> = listOf(
        HTTPHeader(name = "Client-Type", content = "Mobile-App"),
        HTTPHeader(name = "CountryCode", content = "SV")
    )
    
    suspend fun publishExternalContributions(attachments: List<GroupedAttachmentPayload>): CustomizedResponse<List<ReportAttachmentGrouping>> {
        return try {
            evidenceService.publishExternalContributions(
                attachments = attachments,
                headers = headers
            )
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.error?.localizedMessage ?: "")
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.response.code)
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.localizedMessage ?: "")
        }
    }
}
