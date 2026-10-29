package app.reportamelo.commons

import app.reportamelo.commons.BuildConfig

object Apis {
    const val USER_AGENT = "Reportamelo/1.0"
    const val CLIENT_TYPE_HEADER = "Client-Type"
    const val CLIENT_TYPE_ANDROID = "Mobile-App"
    const val CLIENT_TYPE_APP = "Mobile-App-Android"
    const val GOOGLE_WEB_CLIENT_ID = "214707515100-p8u4l62dlv7120v4dmfogp7hjn74f2c8.apps.googleusercontent.com"

//    const val GOOGLE_WEB_CLIENT_ID = "214707515100-7mdic324baf7jnkeo22jbpsn2ahqijdp.apps.googleusercontent.com"

    val baseURL: String
        get() = "https://${BuildConfig.API_URL}"

    val apiV1: String
        get() = "$baseURL/v1"

    val shareableURL: String
        get() = "https://${BuildConfig.SHARE_URL}"
}
