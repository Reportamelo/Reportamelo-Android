package app.reportamelo.repository

import app.reportamelo.models.*
import app.reportamelo.commons.*
import app.reportamelo.network.*

object VotingRepository {
    private val service = VotingService()
    private val headers = mapOf(
        "Client-Type" to "Mobile-App",
        "CountryCode" to "SV"
    )

    suspend fun vote(type: VotingType, payload: Voting): SuccessfulResult {
        return try {
            val result = service.vote(type, payload, headers)
            if (result.code == "OK") {
                SuccessfulResult.DONE
            } else {
                throw CommonIntercommunicationErrors.UnProcessable
            }
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.localizedMessage ?: "Generic Error")
        }
    }

    suspend fun havIVoted(type: VotingType, resourceId: String): VotingResolution {
        return try {
            val result = service.havIVoted(type, resourceId, headers)
            result.data
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.localizedMessage ?: "Generic Error")
        }
    }
}
