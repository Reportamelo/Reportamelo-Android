package app.reportamelo.repository

import app.reportamelo.models.Comment
import app.reportamelo.models.PaginatedResponse
import app.reportamelo.models.PaginatedRequestQueryParams
import app.reportamelo.network.CommentsService
import app.reportamelo.network.HTTPHeader
import app.reportamelo.commons.CommonIntercommunicationErrors
import app.reportamelo.commons.ServiceError
import app.reportamelo.commons.SuccessfulResult

typealias Comments = PaginatedResponse<Comment>

object CommentsRepository {
    
    private val commentsService = CommentsService()

    suspend fun list(reportId: String, page: Int, limit: Int = 3): Comments {
        return try {
            val query = PaginatedRequestQueryParams(page = page, limit = limit)
            commentsService.list(reportId = reportId, q = query)
        } catch (e: ServiceError.Unauthorized) {
            throw CommonIntercommunicationErrors.GenericError("Unauthorized")
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "Unknown error")
        }
    }

    suspend fun listByUser(page: Int): Comments {
        return try {
            commentsService.listByUser(q = PaginatedRequestQueryParams(page = page, limit = 20))
        } catch (e: ServiceError.Unauthorized) {
            throw CommonIntercommunicationErrors.GenericError("Unauthorized")
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "Unknown error")
        }
    }

    suspend fun post(comment: Comment): SuccessfulResult {
        return try {
            val headers = listOf(
                HTTPHeader(name = "country", content = "country"),
                HTTPHeader(name = "City", content = "city")
            )
            val result = commentsService.post(comment = comment, headers = headers)
            
            if (result.code == "COMMENT_CREATED") {
                SuccessfulResult.DONE
            } else {
                throw CommonIntercommunicationErrors.GenericError(result.code)
            }
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "Network error")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "Unknown error")
        }
    }

    suspend fun update(comment: Comment): SuccessfulResult {
        return try {
            val result = commentsService.update(comment = comment)
            if (result.code == "COMMENT_UPDATED") {
                SuccessfulResult.UPDATED
            } else {
                throw CommonIntercommunicationErrors.GenericError(result.code)
            }
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "Network error")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "Unknown error")
        }
    }

    suspend fun delete(id: String): SuccessfulResult {
        return try {
            val result = commentsService.delete(id = id)
            if (result.code == "COMMENT_DELETED") {
                SuccessfulResult.DELETED
            } else {
                throw CommonIntercommunicationErrors.GenericError(result.code)
            }
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "Network error")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "Unknown error")
        }
    }
}
