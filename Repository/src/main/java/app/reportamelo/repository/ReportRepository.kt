package app.reportamelo.repository

import app.reportamelo.models.*
import app.reportamelo.commons.*
import app.reportamelo.network.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

typealias ErrorHandler = (Throwable) -> Unit

object ReportRepository {
    private val reportsService = ReportsService()

    var headers = listOf(
        HTTPHeader("Client-Type", "Mobile-App"),
        HTTPHeader("CountryCode", "SV")
    )

    // Stubbing local db for MyReportDAOEntity
    private val cachedReportsDAO = mutableMapOf<Int, PaginatedResponse<ReportDAO>>()

    suspend fun start(): StartReportResponse {
        return try {
            reportsService.start(headers)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun deleteTemporalPicture(reportContainer: String, key: String): SuccessfulResult {
        return try {
            val h = listOf(
                HTTPHeader("Client-Type", "Mobile-App"),
                HTTPHeader("CountryCode", "SV"),
                HTTPHeader("Report-Container", reportContainer)
            )
            val result = reportsService.deleteTemporalPicture(key, h)

            if (result.code == "MEDIA_DELETED_SUCCESSFULLY") {
                SuccessfulResult.DELETED
            } else {
                throw CommonIntercommunicationErrors.GenericError(result.message)
            }
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "")
        }
    }

    suspend fun listReports(onError: ErrorHandler): List<Report> {
        return try {
            reportsService.fetchReports()
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    fun listByUser(page: Int): Flow<PaginatedResponse<ReportDAO>> = flow {
        try {
            val cachedResponse = cachedReportsByUser(page)
            if (cachedResponse != null && cachedResponse.documents?.isNotEmpty() == true) {
                emit(cachedResponse)
            }

            val freshResponse = reportsService.fetchReportByUser(
                PaginatedRequestQueryParams(page = page, limit = 5)
            )

            saveReportsByUser(freshResponse, cachedResponse, page)
            emit(freshResponse)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    private suspend fun cachedReportsByUser(page: Int): PaginatedResponse<ReportDAO>? {
        return cachedReportsDAO[page]
    }

    private suspend fun saveReportsByUser(
        freshResponse: PaginatedResponse<ReportDAO>,
        cachedResponse: PaginatedResponse<ReportDAO>?,
        page: Int
    ) {
        cachedReportsDAO[page] = freshResponse
    }

    suspend fun listByProfile(profileId: String): List<ReportDAO> {
        return try {
            reportsService.fetchReportsByProfile(profileId, headers)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun delete(reportId: String): SuccessfulResult {
        return try {
            val result = reportsService.deleteReport(reportId)
            if (result.code == "REPORT_DELETED_SUCCESSFULLY") {
                SuccessfulResult.DELETED
            } else {
                throw CommonIntercommunicationErrors.GenericError("Error deleting report")
            }
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun create(model: ReportDataModel): String {
        return try {
            val h = listOf(
                HTTPHeader("CountryCode", model.locator.countryCode),
                HTTPHeader("CityId", model.locator.cityId),
                HTTPHeader("ShareIndexHash", model.reportSession.shareIndexHash),
                HTTPHeader("ReportContainer", model.reportSession.reportContainer),
                HTTPHeader("GroupingNameCode", model.locator.groupingNameCode),
                HTTPHeader("GroupingCode", model.locator.groupingId)
            )
            val response = reportsService.createReport(model.report, h)

            if (response.code == "REPORT_CREATED") {
                response.id
            } else {
                throw CommonIntercommunicationErrors.GenericError("Error creating report")
            }
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "")
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.result.code)
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun update(report: Report): SuccessfulResult {
        return try {
            val id = report.id ?: throw CommonIntercommunicationErrors.GenericError("No report id")

            if (report.reportState == ReportState.MODIFYING) {
                val response = reportsService.updateReport(id, report, headers)

                if (response.code == "REPORT_UPDATED") {
                    SuccessfulResult.UPDATED
                } else {
                    throw CommonIntercommunicationErrors.GenericError("Error updating report")
                }
            } else {
                throw CommonIntercommunicationErrors.GenericError("Error updating report")
            }
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun submitGroupedAttachments(attachments: List<GroupedAttachmentPayload>): CustomizedResponse<List<ReportAttachmentGrouping>> {
        return try {
            val response = reportsService.submitGroupedAttachments(attachments, headers)
            if (response.code == "ATTACHMENTS_SAVED") {
                response
            } else {
                throw CommonIntercommunicationErrors.GenericError("Error submitting report attachments")
            }
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "")
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.result.code)
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun fetchResolutionByReport(reportId: String): Resolution {
        return try {
            reportsService.fetchResolutionByReport(reportId, headers)
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "")
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.result.code)
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun haveReportBeenValidatedByMe(reportId: String): Boolean {
        if (reportId.isNotEmpty() && reportId == "SV-SS-260601-aXWsaxls") {
            return true
        }
        return false
    }

    suspend fun boostReportValidation(reportId: String): GenericResponse {
        return try {
            reportsService.boostReportValidation(reportId, headers)
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "")
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.result.message)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }

    suspend fun fetchAttachments(reportId: String, page: Int): PaginatedResponse<PreviewAttachment> {
        return try {
            val query = PaginatedRequestQueryParams(page = page, limit = 12)
            reportsService.fetchAttachments(reportId, query, headers)
        } catch (e: ServiceError.NetworkError) {
            throw CommonIntercommunicationErrors.NetworkError(e.message ?: "")
        } catch (e: ServiceError.BadRequest) {
            throw CommonIntercommunicationErrors.InvalidPetition(e.result.code)
        } catch (e: ServiceError.ServerError) {
            throw CommonIntercommunicationErrors.ServerError(e.code)
        } catch (e: Exception) {
            throw CommonIntercommunicationErrors.GenericError(e.message ?: "")
        }
    }
}

sealed class CustomError(message: String) : Exception(message) {
    object MissingId : CustomError("Missing ID")
    object InvalidState : CustomError("Invalid State")
}
