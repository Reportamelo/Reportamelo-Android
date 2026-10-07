package app.reportamelo.commons

import kotlinx.serialization.Serializable

@Serializable
enum class DeviceType(val description: String) {
    IOS("ios"),
    ANDROID("android"),
    WEB("web")
}

@Serializable
enum class CommentForType(val rawValue: String) {
    REPORT("report"),
    PETITION("petition")
}
