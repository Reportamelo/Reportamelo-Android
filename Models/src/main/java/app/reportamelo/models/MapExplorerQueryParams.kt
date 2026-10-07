package app.reportamelo.models

import kotlinx.serialization.Serializable

@Serializable
data class MapExplorerQueryParams(
    val lat: Double,
    val lng: Double,
    val radius: Int,
    val issueTypeIds: List<Int>,
    val severityIds: List<Int>,
    val statusIds: List<Int>
)
