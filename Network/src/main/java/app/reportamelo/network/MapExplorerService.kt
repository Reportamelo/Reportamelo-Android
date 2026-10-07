package app.reportamelo.network

import app.reportamelo.commons.Apis
import app.reportamelo.models.MapExplorerQueryParams
import app.reportamelo.models.MapExplorerReport

class MapExplorerService(
    private val client: ServiceClient = ServiceClient(
        baseURL = Apis.apiV1, 
        decoderType = DecoderType.MESSAGE_PACK
    )
) {
    suspend fun reports(
        q: MapExplorerQueryParams,
        headers: Map<String, String> = emptyMap()
    ): List<MapExplorerReport> {
        return client.gets(
            path = "map-explorer/reports",
            query = q,
            headers = headers,
            withOAuth = false
        )
    }

    suspend fun report(
        id: String,
        headers: Map<String, String> = emptyMap()
    ): MapExplorerReport {
        return client.get(
            path = "map-explorer/reports/$id",
            headers = headers,
            withOAuth = false
        )
    }
}
