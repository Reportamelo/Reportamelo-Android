package app.reportamelo.repository

import app.reportamelo.models.*
import app.reportamelo.commons.*
import app.reportamelo.network.*

object CommentsRepository {

    private val service = app.reportamelo.network.CommentsService(ServiceClient("https://api.reportamelo.app/v1/"))

    suspend fun getCommentsByReport(reportId: String, page: Int): PaginatedResponse<Comment> {
        return try {
            val q = PaginatedRequestQueryParams(page = page, limit = 15)
            service.list(reportId, q)
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.response.code)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
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
