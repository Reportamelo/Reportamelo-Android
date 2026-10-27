package app.reportamelo.locator.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "cantons",
    foreignKeys = [
        ForeignKey(
            entity = District::class,
            parentColumns = ["cityId"],
            childColumns = ["cityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("cityId")
    ]
)
data class Canton(
    @PrimaryKey val code: String,
    val name: String?,
    val cityId: String?
)
