package app.reportamelo.commons

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DeviceType(val description: String) {
    @SerialName("ios") IOS("ios"),
    @SerialName("android") ANDROID("android"),
    @SerialName("web") WEB("web")
}

@Serializable
enum class CommentForType(val rawValue: String) {
    @SerialName("report") REPORT("report"),
    @SerialName("petition") PETITION("petition")
}
