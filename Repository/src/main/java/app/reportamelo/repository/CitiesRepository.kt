package app.reportamelo.repository

import app.reportamelo.models.*
import app.reportamelo.commons.*
import app.reportamelo.network.*

object CitiesRepository {

    private val service = app.reportamelo.network.CityService(ServiceClient("https://api.reportamelo.app/v1/"))

    suspend fun filter(q: PaginatedRequestQueryParams): FriendlyCities {
        return try {
            service.filter(q)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }
}
