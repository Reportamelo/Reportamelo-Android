package app.reportamelo.network.services

import app.reportamelo.commons.Apis
import app.reportamelo.models.*
import app.reportamelo.network.ServiceClient

class ShareService(
    private val client: ServiceClient = ServiceClient(Apis.apiV1)
) {
    suspend fun <T> createLink(share: Share<T>, headers: Map<String, String>): CustomizedResponse<ShareUrlResponse> {
        return client.post("actions/create/shareable/link", share, headers, withOAuth = true)
    }
}
