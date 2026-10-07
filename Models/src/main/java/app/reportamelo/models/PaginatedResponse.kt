package app.reportamelo.models

import kotlinx.serialization.Serializable

@Serializable
data class PaginatedResponse<T>(
    val documents: List<T>? = emptyList(),
    val total: Int? = null,
    val page: Int? = null,
    val documentsPerPage: Int? = null,
    val totalPages: Int? = null,
    val hasNext: Boolean = false,
    val hasPrev: Boolean = false
)

typealias FriendlyCities = PaginatedResponse<FriendlyCityDistribution>
