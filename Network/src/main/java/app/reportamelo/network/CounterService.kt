package app.reportamelo.network

import app.reportamelo.commons.Apis
import app.reportamelo.models.CustomizedResponse
import app.reportamelo.models.ReportCounter

class CounterService(
    private val client: ServiceClient = ServiceClient(baseURL = Apis.apiV1)
) {
    suspend fun increase(headers: Map<String, String> = emptyMap()): CustomizedResponse<ReportCounter> {
        return client.post(path = "report-counter/increase", body = emptyMap<String, String>(), withOAuth = true)
    }
    
    suspend fun count(headers: Map<String, String> = emptyMap()): CustomizedResponse<ReportCounter> {
        return client.get(path = "report-counter", withOAuth = true)
    }
}
