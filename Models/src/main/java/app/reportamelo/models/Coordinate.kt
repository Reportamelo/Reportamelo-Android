package app.reportamelo.models

import kotlinx.serialization.Serializable

@Serializable
data class Coordinate(
    val lat: Double = 0.0,
    val lng: Double = 0.0
)
