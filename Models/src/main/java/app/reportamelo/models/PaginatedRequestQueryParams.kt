package app.reportamelo.models

import kotlinx.serialization.Serializable

@Serializable
data class PaginatedRequestQueryParams(
    val page: Int? = 1,
    val limit: Int? = 3,
    val issueTypeId: Int? = null,
    val severityId: Int? = null,
    val countryCode: String? = null,
    val departmentalCapital: Boolean? = null,
    val cityName: String? = null,
    val stateName: String? = null,
    val groupingName: String? = null,
    val ordering: String = "desc" // mapped from OrderFilter.descending
)
