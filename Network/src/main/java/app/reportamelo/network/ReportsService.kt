package app.reportamelo.network

import app.reportamelo.models.*
import kotlinx.serialization.json.JsonPrimitive

class ReportsService(private val client: ServiceClient = ServiceClient(baseURL = app.reportamelo.commons.Apis.apiV1)) {

    suspend fun start(headers: Map<String, String>): StartReportResponse {
        return client.get(
            path = "reports/start",
            headers = headers,
            withOAuth = true
        )
    }

    suspend fun fetchReports(): List<Report> {
        return client.get(
            path = "reports/",
            withOAuth = true
        )
    }

    suspend fun fetchReport(reportId: String): Report {
        return client.get(
            path = "reports/$reportId",
            withOAuth = true
        )
    }

    private fun toReportDAO(report: Report): ReportDAO {
        val serializedAttachments = if (report.attachmentRequests.isNotEmpty()) {
            report.attachmentRequests.map {
                client.json.encodeToJsonElement(PreviewAttachmentRequest.serializer(), it)
            }
        } else {
            report.attachments.map { JsonPrimitive(it) }
        }
        return ReportDAO(
            id = report.id,
            coordinate = report.coordinate,
            lat = report.lat,
            lng = report.lng,
            address = report.address.ifEmpty { report.zipCode },
            title = report.title.ifEmpty { report.suggestedTitle },
            description = report.description.ifEmpty { report.suggestedDescription },
            issueTypeId = report.issueTypeId,
            severityId = report.severityId,
            statusId = report.statusId,
            matterToSolveId = report.matterToSolveId,
            reportedAt = report.reportedAt?.let { JsonPrimitive(it) },
            cellIndex = report.cellIndex,
            createdAt = report.createdAt?.let { JsonPrimitive(it) },
            updatedAt = report.updatedAt?.let { JsonPrimitive(it) },
            zipCode = report.zipCode,
            groupingId = report.groupingId,
            reportedBy = report.reportedBy,
            olc = report.olc,
            suggestedTitle = report.suggestedTitle,
            suggestedDescription = report.suggestedDescription,
            reportState = report.reportState,
            attachments = serializedAttachments,
            cityId = report.cityId,
            reportContainer = report.reportContainer,
            shareUrl = report.shareUrl,
            observations = report.observations
        )
    }

    suspend fun createReport(report: Report, headers: Map<String, String>): GenericResponse {
        return client.post(
            path = "reports/create",
            body = toReportDAO(report),
            headers = headers,
            withOAuth = true
        )
    }

    suspend fun deleteReport(reportId: String): GenericResponse {
        return client.delete(
            path = "reports/$reportId",
            body = emptyMap<String, String>(),
            headers = emptyMap(),
            withOAuth = true
        )
    }

    suspend fun uploadSinglePicture(
        reportContainer: String,
        imageData: ByteArray
    ): CustomizedResponse<AttachMediaResponse> {
        return client.postMultipart(
            path = "attach-media/upload",
            body = emptyMap(),
            files = listOf(
                MultipartFormFile(
                    name = "picture",
                    filename = "report-picture.webp",
                    mimeType = "image/webp",
                    data = imageData
                )
            ),
            headers = mapOf(
                "report-container" to reportContainer,
                "upload-mode" to "one-by-one"
            ),
            withOAuth = true
        )
    }

    suspend fun deleteTemporalPicture(key: String, headers: Map<String, String>): GenericResponse {
        return client.delete(
            path = "attach-media/$key",
            body = emptyMap<String, String>(),
            headers = headers,
            withOAuth = true
        )
    }

    suspend fun fetchReportByUser(q: PaginatedRequestQueryParams): PaginatedResponse<ReportDAO> {
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
            path = "reports/byUser",
            query = queryParams,
            headers = emptyMap(),
            withOAuth = true
        )
    }

    suspend fun updateReport(reportId: String, report: Report, headers: Map<String, String>): GenericResponse {
        return client.patch(
            path = "reports/$reportId",
            body = toReportDAO(report),
            headers = headers,
            withOAuth = true
        )
    }

    suspend fun submitGroupedAttachments(
        attachments: List<GroupedAttachmentPayload>,
        headers: Map<String, String>
    ): CustomizedResponse<List<ReportAttachmentGrouping>> {
        return client.post(
            path = "report-attachments/group/by/container",
            body = attachments,
            headers = headers,
            withOAuth = true
        )
    }

    suspend fun fetchResolutionByReport(reportId: String, headers: Map<String, String>): Resolution {
        return client.get(
            path = "resolutions/byReportId/$reportId",
            headers = headers,
            withOAuth = true
        )
    }

    suspend fun boostReportValidation(reportId: String, headers: Map<String, String>): GenericResponse {
        return client.patch(
            path = "reports/validate/byUsers/$reportId",
            body = emptyMap<String, String>(),
            headers = headers,
            withOAuth = true
        )
    }

    suspend fun fetchAttachments(
        reportId: String,
        q: PaginatedRequestQueryParams,
        headers: Map<String, String>
    ): PaginatedResponse<PreviewAttachment> {
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
            path = "report-attachments/report/$reportId",
            query = queryParams,
            headers = headers,
            withOAuth = true
        )
    }

    suspend fun fetchReportsByProfile(id: String, headers: Map<String, String>): List<ReportDAO> {
        return client.get(
            path = "reports/byProfile/$id",
            headers = headers,
            withOAuth = true
        )
    }
}
