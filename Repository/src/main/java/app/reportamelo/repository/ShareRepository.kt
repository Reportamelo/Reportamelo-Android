package app.reportamelo.repository

import app.reportamelo.models.*
import app.reportamelo.commons.*
import app.reportamelo.network.*
import app.reportamelo.network.services.*

object ShareRepository {
    private val service = ShareService()

    suspend fun createShareableLink(payload: ReportToShare): String {
        return try {
            val share = Share(payload, shareType = ShareType.report, lang = "es-419")

            val result = service.createLink(
                share,
                headers = mapOf("CountryCode" to "SV")
            )

            if (result.code == "SHARE_GENERATED") {
                result.data?.shareUrl ?: throw CommonIntercommunicationErrors.GenericError("Missing shareUrl")
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
