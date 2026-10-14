package app.reportamelo.commons.validators

fun isAValidText(text: String): Boolean {
    val pattern = "^[a-zA-Z0-9\\. \\-#]+$"
    return text.matches(pattern.toRegex())
}
