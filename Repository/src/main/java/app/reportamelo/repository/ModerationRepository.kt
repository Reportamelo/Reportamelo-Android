package app.reportamelo.repository

import app.reportamelo.models.*
import app.reportamelo.commons.*
import app.reportamelo.network.*

object ModerationRepository {

    private val service = ModerationService()
    var headers = listOf(
        HTTPHeader("Client-Type", "Mobile-App"),
        HTTPHeader("CountryCode", "SV"),
        HTTPHeader("CityId", "san-salvador")
    )

    suspend fun myComplaints(): PaginatedResponse<ReportViolation<ModeratedContent>> {
        return try {
            service.myComplaints(headers)
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.result.code)
        } catch (e: ServiceError.NotFound) {
            throw CommonIntercommunicationErrors.NotFound
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun indictedModeratedContent(type: TypeOfContentToReport): PaginatedResponse<ReportViolation<PreviewAttachment>> {
        return try {
            service.indictedModeratedContent(type, headers)
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.result.code)
        } catch (e: ServiceError.NotFound) {
            throw CommonIntercommunicationErrors.NotFound
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun indictedModeratedMessages(type: TypeOfContentToReport): PaginatedResponse<ReportViolation<CommentToBlock>> {
        return try {
            service.indictedModeratedMessages(type, headers)
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.result.code)
        } catch (e: ServiceError.NotFound) {
            throw CommonIntercommunicationErrors.NotFound
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun moderateContent(reason: ReportViolation<PreviewAttachment>, type: TypeOfContentToReport): GenericResponse {
        return try {
            service.moderateContent(reason, type, headers)
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.result.code)
        } catch (e: ServiceError.NotFound) {
            throw CommonIntercommunicationErrors.NotFound
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun moderateComment(reason: ReportViolation<CommentToBlock>, type: TypeOfContentToReport): GenericResponse {
        return try {
            service.moderateComment(reason, type, headers)
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.result.code)
        } catch (e: ServiceError.NotFound) {
            throw CommonIntercommunicationErrors.NotFound
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun moderateAccount(reason: ReportViolation<User>, type: TypeOfContentToReport): GenericResponse {
        return try {
            service.moderateAccount(reason, type, headers)
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.result.code)
        } catch (e: ServiceError.NotFound) {
            throw CommonIntercommunicationErrors.NotFound
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun appeal(id: String, type: TypeOfContentToReport): GenericResponse {
        return try {
            service.appeal(id, type, headers)
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.result.code)
        } catch (e: ServiceError.NotFound) {
            throw CommonIntercommunicationErrors.NotFound
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun remove(id: String, type: TypeOfContentToReport): GenericResponse {
        return try {
            service.remove(id, type, headers)
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.result.code)
        } catch (e: ServiceError.NotFound) {
            throw CommonIntercommunicationErrors.NotFound
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun indictedModeratedContent(): PaginatedResponse<ReportViolation<ModeratedContent>> {
        return try {
            service.indictedModeratedContent(headers)
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.result.code)
        } catch (e: ServiceError.NotFound) {
            throw CommonIntercommunicationErrors.NotFound
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun moderateReport(reason: ReportViolation<MapExplorerReport>, type: TypeOfContentToReport): GenericResponse {
        return try {
            service.moderateReport(reason, type, headers)
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.result.code)
        } catch (e: ServiceError.NotFound) {
            throw CommonIntercommunicationErrors.NotFound
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "")
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }
}
