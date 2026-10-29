package app.reportamelo.repository

import app.reportamelo.models.*
import app.reportamelo.commons.*
import app.reportamelo.network.*

object CommentsRepository {

    var service: CommentsService = CommentsService(ServiceClient(Apis.apiV1))

    suspend fun list(reportId: String, page: Int, limit: Int = 3): PaginatedResponse<Comment> {
        return try {
            val query = PaginatedRequestQueryParams(page = page, limit = limit)
            service.list(reportId, query)
        } catch (e: CommonIntercommunicationErrors) {
            throw e
        } catch (e: ServiceError.Unauthorized) {
            throw CommonIntercommunicationErrors.GenericError("Unauthorized")
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.message ?: "Server Error")
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.response.code)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun listByUser(page: Int): PaginatedResponse<Comment> {
        return try {
            service.listByUser(PaginatedRequestQueryParams(page = page, limit = 20))
        } catch (e: CommonIntercommunicationErrors) {
            throw e
        } catch (e: ServiceError.Unauthorized) {
            throw CommonIntercommunicationErrors.GenericError("Unauthorized")
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.message ?: "Server Error")
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.response.code)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun post(comment: Comment): SuccessfulResult {
        return try {
            val headers = mapOf(
                "country" to "country",
                "City" to "city"
            )
            val result = service.post(comment, headers)
            if (result.code == "COMMENT_CREATED") {
                SuccessfulResult.DONE
            } else {
                throw CommonIntercommunicationErrors.GenericError(result.code)
            }
        } catch (e: CommonIntercommunicationErrors) {
            throw e
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "Network Error")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun update(comment: Comment): SuccessfulResult {
        return try {
            val result = service.update(comment)
            if (result.code == "COMMENT_UPDATED") {
                SuccessfulResult.UPDATED
            } else {
                throw CommonIntercommunicationErrors.GenericError(result.code)
            }
        } catch (e: CommonIntercommunicationErrors) {
            throw e
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "Network Error")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun delete(id: String): SuccessfulResult {
        return try {
            val result = service.delete(id)
            if (result.code == "COMMENT_DELETED") {
                SuccessfulResult.DELETED
            } else {
                throw CommonIntercommunicationErrors.GenericError(result.code)
            }
        } catch (e: CommonIntercommunicationErrors) {
            throw e
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "Network Error")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun getCommentsByReport(reportId: String, page: Int): PaginatedResponse<Comment> {
        return list(reportId = reportId, page = page, limit = 15)
    }

    suspend fun createCommentForReport(comment: Comment): GenericResponse {
        return try {
            service.post(comment, mapOf("CountryCode" to "SV"))
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.response.code)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }
}
