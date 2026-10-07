package app.reportamelo.models

import kotlinx.serialization.Serializable

@Serializable
data class GenericResponse(
    val id: String = "",
    val message: String = "",
    val code: String = ""
)
