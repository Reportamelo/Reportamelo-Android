package app.reportamelo.repository

import app.reportamelo.network.CounterService
import app.reportamelo.network.HTTPHeader
import app.reportamelo.commons.CommonIntercommunicationErrors
import app.reportamelo.commons.ServiceError

object CounterRepository {
    
    private val service = CounterService()
    
    var headers: List<HTTPHeader> = listOf(
        HTTPHeader(name = "Client-Type", content = "Mobile-App"),
        HTTPHeader(name = "CountryCode", content = "SV")
    )

    suspend fun increase() {
        try {
            service.increase(headers)
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "Network Error")
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "Unknown error")
        }
    }

    suspend fun count(): Int? {
        return try {
            val response = service.count(headers)
            response.data.count
        } catch (e: Exception) {
            null
        }
    }
}
