package app.reportamelo.repository

import app.reportamelo.models.*
import app.reportamelo.commons.*
import app.reportamelo.network.*

object InsightsRepository {

    private val service = app.reportamelo.network.InsightsService(app.reportamelo.network.ServiceClient("https://api.reportamelo.app/v1/"))
    var headers = mapOf("CountryCode" to "SV", "CityId" to "san-salvador")

    suspend fun getMonthlyInsights(filter: InsightsFilter): MonthlyInsightsResponse {
        return try {
            service.getMonthlyInsights(filter)
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.response.code)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }
}
