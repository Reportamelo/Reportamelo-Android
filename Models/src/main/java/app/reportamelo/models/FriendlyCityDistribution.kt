package app.reportamelo.models

import kotlinx.serialization.Serializable

@Serializable
data class FriendlyCityDistribution(
    val cityId: String,
    val firstLevel: String,
    val secondLevel: String,
    val thirdLevel: String,
    val ZipCode: String? = null,
    val legalGroupName: String,
    val coordinates: Coordinate,
    val isCapitalCity: Int? = null,
    val isDepartmentalCapital: Int? = null,
    val groupingId: String? = null
)
