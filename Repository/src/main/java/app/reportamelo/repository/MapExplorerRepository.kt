package app.reportamelo.repository

import app.reportamelo.models.*
import app.reportamelo.commons.*
import app.reportamelo.network.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object MapExplorerRepository {

    private val service = MapExplorerService()

    // Stubbing local database using in-memory cache as DAO does not exist
    private val cachedReports = mutableMapOf<String, MapExplorerReport>()

    suspend fun cachedReports(countryCode: CountryCode, cityId: String): List<MapExplorerReport> {
        return cachedReports.values.filter { it.cityId == cityId }
    }

    suspend fun saveReports(
        reports: List<MapExplorerReport>,
        cachedReportsList: List<MapExplorerReport>,
        query: MapExplorerQueryParams,
        cityId: String
    ) {
        val freshIds = reports.map { it.id }.toSet()

        // 1. Delete ghost records
        for (cached in cachedReportsList) {
            if (!freshIds.contains(cached.id)) {
                val distance = calculateDistance(query.lat, query.lng, cached.lat, cached.lng)
                val matchesFilter = distance <= query.radius &&
                        query.issueTypeIds.contains(cached.issueTypeId) &&
                        query.severityIds.contains(cached.severityId) &&
                        query.statusIds.contains(cached.statusId)
                if (matchesFilter) {
                    this.cachedReports.remove(cached.id)
                }
            }
        }

        // 3. Upsert reports
        for (report in reports) {
            this.cachedReports[report.id] = report
        }
    }

    suspend fun report(id: String, countryCode: CountryCode, cityId: String): MapExplorerReport {
        return try {
            val headers = listOf(
                HTTPHeader("countryCode", countryCode.rawValue),
                HTTPHeader("cityId", cityId),
                HTTPHeader("Content-Type", "application/x-msgpack")
            )
            service.report(id, headers)
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.result.message)
        } catch (e: ServiceError.NotFound) {
            throw CommonIntercommunicationErrors.NotFound
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    fun listReportsStream(
        q: MapExplorerQueryParams,
        countryCode: CountryCode,
        cityId: String
    ): Flow<List<MapExplorerReport>> = flow {
        try {
            val cached = cachedReports(countryCode, cityId)

            val filteredCached = cached.filter { report ->
                val distance = calculateDistance(q.lat, q.lng, report.lat, report.lng)
                distance <= q.radius &&
                        q.issueTypeIds.contains(report.issueTypeId) &&
                        q.severityIds.contains(report.severityId) &&
                        q.statusIds.contains(report.statusId)
            }

            if (filteredCached.isNotEmpty()) {
                emit(filteredCached)
            }

            val freshReports = listReports(q, countryCode, cityId)
            saveReports(freshReports, cached, q, cityId)

            emit(freshReports)
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun listReports(
        q: MapExplorerQueryParams,
        countryCode: CountryCode,
        cityId: String
    ): List<MapExplorerReport> {
        return try {
            val headers = listOf(
                HTTPHeader("countryCode", countryCode.rawValue),
                HTTPHeader("cityId", cityId),
                HTTPHeader("Content-Type", "application/x-msgpack")
            )
            service.reports(q, headers)
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.result.code)
        } catch (e: ServiceError.NotFound) {
            throw CommonIntercommunicationErrors.NotFound
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }
}
