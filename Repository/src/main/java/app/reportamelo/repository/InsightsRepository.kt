package app.reportamelo.repository

import app.reportamelo.commons.*
import app.reportamelo.models.*
import app.reportamelo.network.*

object InsightsRepository {
    
    val service: InsightsService = InsightsService()
    
    suspend fun initialize(): SuccessfulResult {
        return try {
            val response = service.initialize()
            if (response.code == "USER_INSIGHTS_INITIALIZED") {
                SuccessfulResult.CREATED
            } else {
                throw CommonIntercommunicationErrors.InvalidPetition(response.message)
            }
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.error.message)
        } catch (e: ServiceError.Unauthorized) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.Forbidden) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.error)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.localizedMessage ?: "")
        }
    }
    
    suspend fun insightsForThisMonth(): MonthlyInsightsResponse {
        return try {
            // Assuming getFullYear() and getMonthName() are utility functions available globally
            val filter = InsightsFilter(year = getFullYear(), month = getMonthName())
            service.getMonthlyInsights(filter)
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.error.message)
        } catch (e: ServiceError.Unauthorized) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.Forbidden) {
            throw CommonIntercommunicationErrors.NotAuthorized
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.error)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.localizedMessage ?: "")
        }
    }
    
    suspend fun filterInsights(filter: InsightsFilter): MonthlyInsightsResponse {
        return service.getMonthlyInsights(filter)
    }
}
