package app.reportamelo.network

import app.reportamelo.commons.Apis
import app.reportamelo.models.FriendlyCities
import app.reportamelo.models.PaginatedRequestQueryParams

class CityService(
    private val client: ServiceClient = ServiceClient(baseURL = Apis.apiV1)
) {
    suspend fun filter(q: PaginatedRequestQueryParams): FriendlyCities {
        return client.gets(path = "cities/filter", query = q, headers = emptyMap(), withOAuth = false)
    }
}
