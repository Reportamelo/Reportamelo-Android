package app.reportamelo.locator.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "cities",
    indices = [
        Index(value = ["geoCode"]),
        Index(value = ["zipCode"]),
        Index(value = ["groupingNameCode"])
    ]
)
data class DistrictEntity(
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
    val geoCode: String?
)

@Entity(
    tableName = "cantons",
    indices = [Index(value = ["cityId"])],
    foreignKeys = [
        ForeignKey(
            entity = DistrictEntity::class,
            parentColumns = ["cityId"],
            childColumns = ["cityId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CantonEntity(
    @PrimaryKey val code: String,
    val name: String?,
    val cityId: String?
)
