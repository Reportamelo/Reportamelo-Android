package app.reportamelo.repository

import app.reportamelo.models.*
import app.reportamelo.commons.*
import app.reportamelo.network.*

object PetitionRepository {

    private val service = app.reportamelo.network.PetitionsService(app.reportamelo.network.ServiceClient(Apis.apiV1))

    suspend fun list(q: PaginatedRequestQueryParams, locator: Any): PaginatedResponse<PetitionPost> {
        return try {
            // Need to fix this locator stuff. It seems locator is an iOS concept that we haven't ported fully.
            // Let's pass empty map for now or dummy headers.
            val headers = mapOf("CountryCode" to "SV", "ThirdLevel" to "dummy")
            service.fetchPetitions(q, headers)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun create(petition: Petition): SuccessfulResult {
        return try {
            val result = service.createPetition(petition)
            if (result.code == "PETITION_CREATED") {
                SuccessfulResult.CREATED
            } else {
                throw CommonIntercommunicationErrors.GenericError(result.message)
            }
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun update(
        petition: Petition,
        onComplete: (GenericResponse) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        try {
            val id = petition.id ?: return
            val result = service.updatePetition(id, petition)
            onComplete(result)
        } catch (e: Exception) {
            onError(e)
        }
    }

    suspend fun listByUser(
        q: PaginatedRequestQueryParams,
        onComplete: (PaginatedResponse<Petition>) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        try {
            val result = service.fetchPetitionsByUser(q)
            onComplete(result)
        } catch (e: Exception) {
            onError(e)
        }
    }

    suspend fun delete(
        petitionId: String,
        onComplete: (GenericResponse) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        try {
            val result = service.deletePetition(petitionId)
            onComplete(result)
        } catch (e: Exception) {
            onError(e)
        }
    }
}
