package app.reportamelo.repository

import app.reportamelo.models.*
import app.reportamelo.commons.*
import app.reportamelo.network.*
import app.reportamelo.network.MapExplorerService

object MapExplorerRepository {

    private val service = MapExplorerService(ServiceClient("https://api.reportamelo.app/v1/"))

    suspend fun reportsByLocation(lat: Double, lng: Double, radius: Int, severityId: Int?, issueTypeId: Int?, cityId: String): List<MapExplorerReport> {
        return try {
            val issueTypes = if (issueTypeId != null) listOf(issueTypeId) else emptyList()
            val severities = if (severityId != null) listOf(severityId) else emptyList()
            val q = MapExplorerQueryParams(lat = lat, lng = lng, radius = radius, issueTypeIds = issueTypes, severityIds = severities, statusIds = emptyList())
            service.reports(q, mapOf("CountryCode" to "SV", "CityId" to cityId))
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.response.code)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun report(id: String, countryCode: String, cityId: String): MapExplorerReport {
        return try {
            service.report(id, mapOf("CountryCode" to countryCode, "CityId" to cityId))
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.response.code)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }
}
