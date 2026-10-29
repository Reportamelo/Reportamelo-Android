package app.reportamelo.repository

import app.reportamelo.commons.*
import app.reportamelo.commons.utils.getFullYear
import app.reportamelo.commons.utils.getMonthName
import app.reportamelo.models.*
import app.reportamelo.network.*

object InsightsRepository {

    val shared: InsightsRepository = this
    var service: InsightsService = InsightsService(ServiceClient(Apis.apiV1))
    var headers = mapOf(
        Apis.CLIENT_TYPE_HEADER to Apis.CLIENT_TYPE_ANDROID,
        "CountryCode" to "SV",
        "CityId" to "san-salvador"
    )

    suspend fun initialize(): SuccessfulResult {
        return try {
            val response = service.initialize()
            if (response.code == "USER_INSIGHTS_INITIALIZED") {
                SuccessfulResult.CREATED
            } else {
                throw CommonIntercommunicationErrors.InvalidPetition(response.message ?: response.code)
            }
        } catch (e: CommonIntercommunicationErrors) {
            throw e
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.response.message ?: e.response.code)
        } catch (e: ServiceError.Unauthorized) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.Forbidden) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun insightsForThisMonth(): MonthlyInsightsResponse {
        val filter = InsightsFilter(year = getFullYear(), month = getMonthName())
        return getMonthlyInsights(filter)
    }

    suspend fun getMonthlyInsights(filter: InsightsFilter): MonthlyInsightsResponse {
        return try {
            service.getMonthlyInsights(filter)
        } catch (e: CommonIntercommunicationErrors) {
            throw e
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.response.message ?: e.response.code)
        } catch (e: ServiceError.Unauthorized) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.Forbidden) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun filterInsights(by: InsightsFilter): MonthlyInsightsResponse {
        return getMonthlyInsights(by)
    }
}
