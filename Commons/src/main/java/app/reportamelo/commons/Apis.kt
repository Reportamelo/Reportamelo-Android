package app.reportamelo.commons

import app.reportamelo.commons.BuildConfig

object Apis {
    val USER_AGENT = "Reportamelo/1.0"

    val baseURL: String
        get() = "https://${BuildConfig.API_URL}"

    val apiV1: String
        get() = "$baseURL/v1"

    val shareableURL: String
        get() = "https://${BuildConfig.SHARE_URL}"
}
