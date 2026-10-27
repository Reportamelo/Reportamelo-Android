package app.reportamelo.locator.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "cities",
    indices = [
        Index("geoCode"),
        Index("zipCode"),
        Index("groupingNameCode")
    ]
)
data class District(
    @PrimaryKey val cityId: String,
    val zipCode: String?,
    val cityNameSortKey: String?,
    val lat: Double,
    val lng: Double,
    val countryCode: String,
    val firstLevel: String?,
    val groupingId: String?,
    val groupingName: String?,
    val groupingNameSortKey: String?,
    val groupingNameCode: String?,
    val isCapitalCity: Boolean,
    val isDepartmentalCapital: Boolean,
    val legalGroupName: String?,
    val secondLevel: String?,
    val stateNameSortKey: String?,
    val thirdLevel: String?,
    val geoCode: String?,
    val cantons: String?
)
