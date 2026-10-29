package app.reportamelo.repository

import app.reportamelo.models.*
import app.reportamelo.commons.*
import app.reportamelo.network.*
import app.reportamelo.network.CounterService

object CounterRepository {
    private val service = CounterService(ServiceClient(Apis.apiV1))

    suspend fun getCount(): CustomizedResponse<ReportCounter> {
        return try {
            service.count(mapOf("CountryCode" to "SV"))
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.response.code)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }
}
