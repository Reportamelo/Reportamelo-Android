package app.reportamelo.locator.data

import androidx.room.Embedded
import androidx.room.Relation

data class DistrictWithCantons(
    @Embedded val district: District,
    @Relation(
        parentColumn = "cityId",
        entityColumn = "cityId"
    )
    val cantons: List<Canton>
)
