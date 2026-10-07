package app.reportamelo.models

import kotlinx.serialization.Serializable

@Serializable
data class District(
    val cityId: String = "",
    val firstLevel: String? = null,
    val secondLevel: String? = null,
    val thirdLevel: String? = null,
    val zipCode: String = "",
    val legalGroupName: String? = null,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val isCapitalCity: Boolean = false,
    val isDepartmentalCapital: Boolean = false,
    val groupingId: String? = null,
    val groupingName: String? = null
)
