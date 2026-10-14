package app.reportamelo.commons.validators

data class Validator(
    val name: String,
    val message: String,
    val fn: (String) -> Boolean
)
