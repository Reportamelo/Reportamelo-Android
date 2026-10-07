package app.reportamelo.network

import app.reportamelo.commons.Apis
import app.reportamelo.commons.VotingType
import app.reportamelo.models.CustomizedResponse
import app.reportamelo.models.GenericResponse
import app.reportamelo.models.Voting
import app.reportamelo.models.VotingResolution

class VotingService(
    private val client: ServiceClient = ServiceClient(baseURL = Apis.apiV1)
) {
    suspend fun vote(votingType: VotingType, payload: Voting, headers: Map<String, String>): GenericResponse {
        return client.post(path = "voting/${votingType.description}", body = payload, headers = headers, withOAuth = true)
    }
    
    suspend fun havIVoted(votingType: VotingType, resourceId: String, headers: Map<String, String>): CustomizedResponse<VotingResolution> {
        return client.get(path = "voting/${votingType.description}/$resourceId", headers = headers, withOAuth = true)
    }
}
