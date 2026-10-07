package app.reportamelo.repository

import app.reportamelo.models.*
import app.reportamelo.commons.*
import app.reportamelo.network.*

object ShareRepository {
    private val service = ShareService()

    suspend fun createShareableLink(model: ReportDataModel): String {
        return try {
            val reportId = model.buildReportId()

            val payload = ReportToShare(
                reportId = reportId,
                title = model.report.title,
                city = model.locator.thirdLevel,
                country = model.locator.firstLevel,
                description = model.report.description,
                severity = model.report.severity.title,
                issueType = model.report.issueType.title,
                status = model.report.status.title,
                coordinate = model.report.coordinate,
                cellIndex = model.report.cellIndex,
                openCodeLocation = model.report.olc ?: "",
                attachments = model.report.attachments
            )

            val share = Share(payload, type = ShareType.report, lang = "es-419")

            val result = service.createLink(
                share,
                headers = listOf(HTTPHeader("CountryCode", "SV"))
            )

            if (result.code == "SHARE_GENERATED") {
                result.data.shareUrl
            } else {
                throw CommonIntercommunicationErrors.InvalidPetition(result.code)
            }
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.message ?: "Server Error")
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "Network Error")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.message ?: "Invalid Petition")
        }
    }
}
