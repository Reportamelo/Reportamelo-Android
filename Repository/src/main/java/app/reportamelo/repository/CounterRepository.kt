package app.reportamelo.repository

import app.reportamelo.models.*
import app.reportamelo.commons.*
import app.reportamelo.network.*
import app.reportamelo.network.CounterService

object CounterRepository {
    private val service = CounterService(ServiceClient(Apis.apiV1))
    private val headers = mapOf(
        Apis.CLIENT_TYPE_HEADER to Apis.CLIENT_TYPE_ANDROID,
        "CountryCode" to "SV"
    )

    suspend fun getCount(): CustomizedResponse<ReportCounter> {
        return try {
            service.count(headers)
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.response.code)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun increase(): CustomizedResponse<ReportCounter> {
        return try {
            service.increase(headers)
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "")
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.response.code)
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }
}
