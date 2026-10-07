package app.reportamelo.network

import app.reportamelo.models.*

class PetitionsService(private val client: ServiceClient = ServiceClient(baseURL = app.reportamelo.commons.Apis.apiV1)) {

    suspend fun fetchPetitions(
        q: PaginatedRequestQueryParams,
        headers: Map<String, String>
    ): PaginatedResponse<PetitionPost> {
        val queryParams = mapOf(
            "page" to q.page?.toString(),
            "limit" to q.limit?.toString(),
            "issueTypeId" to q.issueTypeId?.toString(),
            "severityId" to q.severityId?.toString(),
            "countryCode" to q.countryCode,
            "departmentalCapital" to q.departmentalCapital?.toString(),
            "cityName" to q.cityName,
            "stateName" to q.stateName,
            "groupingName" to q.groupingName,
            "ordering" to q.ordering
        ).filterValues { it != null } as Map<String, String>

        return client.gets(
            path = "petitions/",
            query = queryParams,
            headers = headers,
            withOAuth = true
        )
    }

    suspend fun fetchPetition(id: Int): Petition {
        return client.get(
            path = "petitions/$id",
            withOAuth = true
        )
    }

    suspend fun createPetition(petition: Petition): GenericResponse {
        return client.post(
            path = "petitions/create",
            body = PetitionDTO(
                id = petition.id,
                title = petition.title,
                description = petition.description,
                targetSignatures = petition.targetSignatures,
                currentSignatures = petition.currentSignatures,
                categoryId = petition.categoryId,
                statusId = petition.statusId,
                reportedBy = petition.reportedBy,
                disabled = petition.disabled,
                reportsIds = petition.reportsIds
            ),
            withOAuth = true
        )
    }

    suspend fun updatePetition(id: String, petition: Petition): GenericResponse {
        return client.put(
            path = "petitions/$id",
            body = petition,
            withOAuth = true
        )
    }

    suspend fun fetchPetitionsByUser(q: PaginatedRequestQueryParams): PaginatedResponse<Petition> {
        val queryParams = mapOf(
            "page" to q.page?.toString(),
            "limit" to q.limit?.toString(),
            "issueTypeId" to q.issueTypeId?.toString(),
            "severityId" to q.severityId?.toString(),
            "countryCode" to q.countryCode,
            "departmentalCapital" to q.departmentalCapital?.toString(),
            "cityName" to q.cityName,
            "stateName" to q.stateName,
            "groupingName" to q.groupingName,
            "ordering" to q.ordering
        ).filterValues { it != null } as Map<String, String>
        return client.gets(
            path = "petitions/byUser",
            query = queryParams,
            withOAuth = true
        )
    }

    suspend fun signPetition(id: String): GenericResponse {
        return client.patch(
            path = "petitions/$id/sign-petition",
            body = emptyMap<String, String>(),
            withOAuth = true
        )
    }

    suspend fun deletePetition(id: String): GenericResponse {
        return client.delete(
            path = "petitions/$id",
            body = emptyMap<String, String>(),
            withOAuth = true
        )
    }
}
