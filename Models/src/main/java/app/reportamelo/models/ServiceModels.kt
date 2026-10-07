package app.reportamelo.models

import app.reportamelo.commons.*
import kotlinx.serialization.Serializable

@Serializable
data class OAuthSignInPayload(
    val token: String
)

@Serializable
data class AuthPayload(
    val token: String,
    val name: String? = null,
    val email: String? = null
)

@Serializable
data class EmptyResponse(val ignored: String? = null) // Sometimes empty objects are tricky in JSON parsing, adding an optional dummy field or leaving empty.

@Serializable
data class Notifications(
    val app: Boolean,
    val email: Boolean,
    val web: Boolean
)

@Serializable
data class PrivacySettings(
    val showMyProfile: Boolean,
    val showMyUseNameWhenShare: Boolean
)

@Serializable
data class ReportLocatorSettings(
    val countryCode: String,
    val cityId: String
)

@Serializable
data class Settings(
    val notifications: Notifications,
    val privacySettings: PrivacySettings,
    val avatarCreatedFrom: AvatarCreatedFrom,
    val reportLocatorSettings: ReportLocatorSettings
)

@Serializable
data class PublicUserData(
    val profileId: String,
    val userId: String,
    val userName: String,
    val names: String,
    val email: String,
    val profilePicture: String,
    val sessionDuration: Int,
    val userType: UserType,
    val settings: Settings,
    val landingPageCompleted: Boolean
)

@Serializable
data class LoginWithOAuthProviderResponse(
    val code: String,
    val authSessionId: String,
    val authProvider: String,
    val publicUserData: PublicUserData
)

@Serializable
data class DefaultReportingCity(
    val cityId: String
)

@Serializable
data class AvatarResponse(
    val avatarUrl: String
)

@Serializable
data class AvatarCreatedFromRequest(
    val avatarCreatedFrom: AvatarCreatedFrom
)

@Serializable
data class BlockUserReason(
    val profileId: String,
    val reason: String,
    val blockedReasonId: String
)

@Serializable
data class DeviceTokenRequest(
    val deviceToken: String,
    val deviceId: String,
    val platform: DeviceType,
    val isActive: Boolean = true
)

@Serializable
data class ReportViolation<T>(
    val id: String? = null,
    val type: TypeOfContentToReport,
    val content: T,
    val contentAuthorProfileId: String,
    val reason: String,
    val blockedReasonId: String,
    val status: ReportViolationStatus,
    val observation: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class ModeratedContent(
    val id: String,
    val commentFor: String? = null,
    val message: String? = null,
    val profileId: String? = null,
    val resourceId: String? = null,
    val createdAtRaw: Long? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val fileName: String? = null,
    val reportContainer: String? = null,
    val state: String? = null,
    val type: String? = null,
    val uploaderUserName: String? = null
)

