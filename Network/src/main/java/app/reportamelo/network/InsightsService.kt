package app.reportamelo.network

import app.reportamelo.models.*

class InsightsService(private val client: ServiceClient = ServiceClient(baseURL = app.reportamelo.commons.Apis.apiV1)) {

    suspend fun initialize(): GenericResponse {
        return client.post(
            path = "insights/stats/initialize",
            body = emptyMap<String, String>(),
            withOAuth = true
        )
    }

    suspend fun getMonthlyInsights(filter: InsightsFilter): MonthlyInsightsResponse {
        return client.get(
            path = "insights/stats/${filter.year}/${filter.month}",
            withOAuth = true
        )
    }
}
