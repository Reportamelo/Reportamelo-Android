package app.reportamelo.network

import app.reportamelo.commons.Apis
import app.reportamelo.models.Comment
import app.reportamelo.models.GenericResponse
import app.reportamelo.models.PaginatedRequestQueryParams
import app.reportamelo.models.PaginatedResponse

open class CommentsService(
    private val client: ServiceClient = ServiceClient(baseURL = Apis.apiV1)
) {
    open suspend fun post(comment: Comment, headers: Map<String, String>): GenericResponse {
        return client.post(path = "comments/create", body = comment, headers = headers, withOAuth = true)
    }
    
    open suspend fun update(comment: Comment): GenericResponse {
        return client.put(path = "comments/${comment.id}", body = comment, withOAuth = true)
    }
    
    open suspend fun delete(id: String): GenericResponse {
        return client.delete(path = "comments/$id", body = emptyMap<String, String>(), withOAuth = true)
    }
    
    open suspend fun listByUser(q: PaginatedRequestQueryParams): PaginatedResponse<Comment> {
        return client.gets(path = "comments/user", query = q, headers = emptyMap(), withOAuth = true)
    }
    
    open suspend fun list(reportId: String, q: PaginatedRequestQueryParams): PaginatedResponse<Comment> {
        return client.gets(path = "comments/report/$reportId", query = q, headers = emptyMap(), withOAuth = true)
    }
}
