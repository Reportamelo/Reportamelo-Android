package app.reportamelo.network

import app.reportamelo.models.*

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

    suspend fun createReport(report: Report, headers: Map<String, String>): GenericResponse {
        return client.post(
            path = "reports/create",
            body = ReportDAO(
                id = report.id,
                lat = report.lat,
                lng = report.lng,
                issueTypeId = report.issueTypeId,
                severityId = report.severityId,
                statusId = report.statusId,
                zipCode = report.zipCode,
                groupingId = report.groupingId,
                reportedBy = report.reportedBy,
                suggestedTitle = report.suggestedTitle,
                suggestedDescription = report.suggestedDescription,
                attachments = report.attachments,
                cityId = report.cityId,
                reportContainer = report.reportContainer,
                shareUrl = report.shareUrl,
                observations = report.observations
            ),
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
            body = ReportDAO(
                id = report.id,
                lat = report.lat,
                lng = report.lng,
                issueTypeId = report.issueTypeId,
                severityId = report.severityId,
                statusId = report.statusId,
                zipCode = report.zipCode,
                groupingId = report.groupingId,
                reportedBy = report.reportedBy,
                suggestedTitle = report.suggestedTitle,
                suggestedDescription = report.suggestedDescription,
                attachments = report.attachments,
                cityId = report.cityId,
                reportContainer = report.reportContainer,
                shareUrl = report.shareUrl,
                observations = report.observations
            ),
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
